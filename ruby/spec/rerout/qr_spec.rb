# frozen_string_literal: true

require 'spec_helper'
require 'uri'

RSpec.describe Rerout::Resources::Qr do
  describe '#url (pure builder)' do
    let(:client) { Rerout::Client.new(api_key: 'rrk_test') }

    it 'builds a bare URL with no options' do
      expect(client.qr.url('q4')).to eq('https://api.rerout.co/v1/links/q4/qr')
    end

    it 'omits the query string entirely when no options are given' do
      expect(client.qr.url('q4')).not_to include('?')
    end

    it 'emits every option when supplied as a QrOptions object' do
      options = Rerout::QrOptions.new(
        size: 12, margin: 2, ecc: 'H', domain: 'go.brand.com', refresh: 'v2'
      )
      url = client.qr.url('q4', options)
      query = URI.decode_www_form(URI.parse(url).query).to_h
      expect(query).to eq(
        'size' => '12', 'margin' => '2', 'ecc' => 'H',
        'domain' => 'go.brand.com', 'refresh' => 'v2'
      )
    end

    it 'accepts a plain Hash of options' do
      url = client.qr.url('q4', { size: 8, ecc: 'M' })
      query = URI.decode_www_form(URI.parse(url).query).to_h
      expect(query).to eq('size' => '8', 'ecc' => 'M')
    end

    it 'serializes refresh: true as 1' do
      url = client.qr.url('q4', Rerout::QrOptions.new(refresh: true))
      expect(URI.parse(url).query).to eq('refresh=1')
    end

    it 'serializes a string refresh value verbatim' do
      url = client.qr.url('q4', Rerout::QrOptions.new(refresh: 'v2'))
      expect(URI.parse(url).query).to eq('refresh=v2')
    end

    it 'honours a custom base URL' do
      staging = Rerout::Client.new(api_key: 'rrk_test', base_url: 'https://staging.rerout.co')
      expect(staging.qr.url('q4')).to eq('https://staging.rerout.co/v1/links/q4/qr')
    end

    it 'trims trailing slashes from a custom base URL' do
      url = described_class.build_url(base_url: 'https://x.rerout.co///', code: 'q4')
      expect(url).to eq('https://x.rerout.co/v1/links/q4/qr')
    end

    it 'URL-encodes a code containing a slash' do
      expect(client.qr.url('go/promo')).to eq('https://api.rerout.co/v1/links/go%2Fpromo/qr')
    end

    it 'URL-encodes a code containing a space' do
      expect(client.qr.url('hello world')).to eq('https://api.rerout.co/v1/links/hello%20world/qr')
    end

    it 'raises ArgumentError on a blank code' do
      expect { client.qr.url('') }.to raise_error(ArgumentError, /code is required/)
    end

    it 'rejects an unsupported ecc level at QrOptions construction' do
      expect { Rerout::QrOptions.new(ecc: 'Z') }.to raise_error(ArgumentError, /ecc must be one of/)
    end
  end

  describe '#svg (network fetch)' do
    let(:svg_body) { '<svg xmlns="http://www.w3.org/2000/svg"></svg>' }

    it 'GETs the QR endpoint with a bearer token and returns the SVG text' do
      client, recorded = build_client(api_key: 'rrk_secret') do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4/qr',
                                  body: svg_body, headers: { 'Content-Type' => 'image/svg+xml' })
      end
      result = client.qr.svg('q4')
      expect(result).to eq(svg_body)
      expect(recorded.first.headers['Authorization']).to eq('Bearer rrk_secret')
    end

    it 'passes QR options as query params on the SVG fetch' do
      client, recorded = build_client do |stubs, rec|
        stubs.get('/v1/links/q4/qr') do |env|
          rec << ReroutTestHelpers::RecordedRequest.new(
            http_method: env.method, url: env.url,
            headers: env.request_headers, body: env.request_body
          )
          [200, { 'Content-Type' => 'image/svg+xml' }, '<svg/>']
        end
      end
      client.qr.svg('q4', Rerout::QrOptions.new(size: 16, ecc: 'H'))
      query = URI.decode_www_form(recorded.first.url.query).to_h
      expect(query).to include('size' => '16', 'ecc' => 'H')
    end

    it 'raises ArgumentError on a blank code' do
      client, = build_client
      expect { client.qr.svg('') }.to raise_error(ArgumentError, /code is required/)
    end

    it 'surfaces a not_found error from the QR endpoint' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/missing/qr', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.qr.svg('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end
end
