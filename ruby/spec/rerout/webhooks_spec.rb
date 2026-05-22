# frozen_string_literal: true

require 'spec_helper'
require 'openssl'

RSpec.describe Rerout::Webhooks do
  let(:secret) { 'whsec_supersecret' }
  let(:body) { '{"event":"link.clicked","data":{"code":"q4"}}' }
  let(:fixed_now) { 1_700_000_000 }
  let(:clock) { -> { fixed_now } }

  # Build a `t=,v1=` header signed exactly the way the server does.
  def sign(raw_body, at: fixed_now, with: secret)
    hex = OpenSSL::HMAC.hexdigest('SHA256', with, "#{at}.#{raw_body}")
    "t=#{at},v1=#{hex}"
  end

  def verify(header, raw_body: body, tolerance: 300, now: clock, key: secret)
    described_class.verify_signature(
      raw_body: raw_body, signature_header: header, secret: key,
      tolerance_seconds: tolerance, now: now
    )
  end

  describe '.verify_signature' do
    it 'accepts a freshly signed payload' do
      expect(verify(sign(body))).to be(true)
    end

    it 'rejects a signature made with a different secret' do
      forged = sign(body, with: 'whsec_wrong')
      expect(verify(forged)).to be(false)
    end

    it 'rejects a body tampered with an extra space' do
      header = sign(body)
      expect(verify(header, raw_body: "#{body} ")).to be(false)
    end

    it 'rejects a payload signed outside the tolerance window' do
      stale = sign(body, at: fixed_now - 600)
      expect(verify(stale, tolerance: 300)).to be(false)
    end

    it 'accepts a payload signed exactly at the tolerance boundary' do
      edge = sign(body, at: fixed_now - 300)
      expect(verify(edge, tolerance: 300)).to be(true)
    end

    it 'accepts a payload signed in the future at the tolerance boundary' do
      edge = sign(body, at: fixed_now + 300)
      expect(verify(edge, tolerance: 300)).to be(true)
    end

    it 'rejects a payload one second past the tolerance boundary' do
      past_edge = sign(body, at: fixed_now - 301)
      expect(verify(past_edge, tolerance: 300)).to be(false)
    end

    it 'skips the timestamp check when tolerance is 0' do
      ancient = sign(body, at: 1)
      expect(verify(ancient, tolerance: 0)).to be(true)
    end

    it 'skips the timestamp check when tolerance is negative' do
      ancient = sign(body, at: 1)
      expect(verify(ancient, tolerance: -10)).to be(true)
    end

    it 'uses Time.now when no clock is injected' do
      header = sign(body, at: Time.now.to_i)
      result = described_class.verify_signature(
        raw_body: body, signature_header: header, secret: secret
      )
      expect(result).to be(true)
    end

    describe 'casing variations' do
      it 'accepts uppercase T= and V1= keys' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{fixed_now}.#{body}")
        expect(verify("T=#{fixed_now},V1=#{hex}")).to be(true)
      end

      it 'accepts mixed-case hex in v1' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{fixed_now}.#{body}").upcase
        expect(verify("t=#{fixed_now},v1=#{hex}")).to be(true)
      end

      it 'tolerates whitespace around segments' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{fixed_now}.#{body}")
        expect(verify(" t=#{fixed_now} , v1=#{hex} ")).to be(true)
      end
    end

    describe 'malformed headers' do
      it 'rejects an empty header' do
        expect(verify('')).to be(false)
      end

      it 'rejects a nil header' do
        expect(verify(nil)).to be(false)
      end

      it 'rejects a garbage header' do
        expect(verify('garbage')).to be(false)
      end

      it 'rejects empty t and bad v1 (t=,v1=abc)' do
        expect(verify('t=,v1=abc')).to be(false)
      end

      it 'rejects a header missing the t segment' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{fixed_now}.#{body}")
        expect(verify("v1=#{hex}")).to be(false)
      end

      it 'rejects a header missing the v1 segment' do
        expect(verify("t=#{fixed_now}")).to be(false)
      end

      it 'rejects a non-numeric t' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{fixed_now}.#{body}")
        expect(verify("t=notanumber,v1=#{hex}")).to be(false)
      end

      it 'rejects a non-positive t' do
        hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "0.#{body}")
        expect(verify("t=0,v1=#{hex}")).to be(false)
      end

      it 'rejects a non-hex v1' do
        expect(verify("t=#{fixed_now},v1=zzzzzzzz")).to be(false)
      end

      it 'rejects an odd-length v1' do
        expect(verify("t=#{fixed_now},v1=abc")).to be(false)
      end

      it 'rejects a v1 of the wrong length even if valid hex' do
        expect(verify("t=#{fixed_now},v1=abcdef")).to be(false)
      end
    end

    describe 'secret validation' do
      it 'rejects an empty secret' do
        expect(verify(sign(body), key: '')).to be(false)
      end

      it 'rejects a nil secret' do
        expect(verify(sign(body), key: nil)).to be(false)
      end
    end

    it 'rejects a nil raw_body' do
      expect(verify(sign(body), raw_body: nil)).to be(false)
    end

    it 'verifies an empty-string body when correctly signed' do
      expect(verify(sign(''), raw_body: '')).to be(true)
    end
  end

  describe 'Rerout.verify_signature module convenience' do
    it 'delegates to Rerout::Webhooks.verify_signature' do
      result = Rerout.verify_signature(
        raw_body: body, signature_header: sign(body), secret: secret, now: clock
      )
      expect(result).to be(true)
    end

    it 'rejects a bad signature through the convenience method' do
      result = Rerout.verify_signature(
        raw_body: body, signature_header: 'garbage', secret: secret
      )
      expect(result).to be(false)
    end
  end
end
