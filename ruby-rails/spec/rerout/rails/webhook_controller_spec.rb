# frozen_string_literal: true

require 'spec_helper'
require 'rack/test'
require 'support/test_app'

RSpec.describe Rerout::Rails::WebhookController do
  include Rack::Test::Methods

  def app
    ReroutRailsTest::Application
  end

  let(:webhook_path) { '/rerout/webhooks' }

  # POST a body to the webhook endpoint with a (by default valid) signature.
  def post_webhook(body, signature: :auto, secret: ReroutRailsHelpers::TEST_SECRET,
                   timestamp: Time.now.to_i)
    header = signature == :auto ? sign_body(body, secret: secret, timestamp: timestamp) : signature
    env = { 'CONTENT_TYPE' => 'application/json' }
    env['HTTP_X_REROUT_SIGNATURE'] = header unless header.nil?
    post(webhook_path, body, env)
  end

  describe '200 — happy path' do
    it 'accepts a valid signed webhook' do
      post_webhook(JSON.generate('event' => 'link.clicked', 'code' => 'q4'))
      expect(last_response.status).to eq(200)
      expect(JSON.parse(last_response.body)).to eq('received' => true, 'event' => 'link.clicked')
    end

    it 'accepts a valid webhook with an unknown event type' do
      post_webhook(JSON.generate('event' => 'something.new'))
      expect(last_response.status).to eq(200)
      expect(JSON.parse(last_response.body)['event']).to eq('something.new')
    end

    it 'accepts a valid webhook with no event field' do
      post_webhook(JSON.generate('code' => 'q4'))
      expect(last_response.status).to eq(200)
      expect(JSON.parse(last_response.body)['event']).to eq('')
    end

    it 'dispatches the catch-all notification' do
      body = JSON.generate('event' => 'link.clicked', 'code' => 'q4')
      events = capture_notifications('rerout.webhook') { post_webhook(body) }
      expect(events.size).to eq(1)
      expect(events.first[:body]).to eq('event' => 'link.clicked', 'code' => 'q4')
    end

    it 'dispatches the event-specific notification' do
      body = JSON.generate('event' => 'qr.scanned', 'code' => 'q4')
      events = capture_notifications('rerout.qr.scanned') { post_webhook(body) }
      expect(events.size).to eq(1)
    end

    it 'exposes the delivering request in the notification payload' do
      body = JSON.generate('event' => 'link.clicked')
      events = capture_notifications('rerout.webhook') { post_webhook(body) }
      expect(events.first[:request]).to respond_to(:headers)
    end
  end

  describe '401 — bad signature' do
    it 'rejects a request with no signature header' do
      post_webhook(JSON.generate('event' => 'link.clicked'), signature: nil)
      expect(last_response.status).to eq(401)
    end

    it 'rejects an empty signature header' do
      post_webhook(JSON.generate('event' => 'link.clicked'), signature: '')
      expect(last_response.status).to eq(401)
    end

    it 'rejects a garbage signature header' do
      post_webhook(JSON.generate('event' => 'link.clicked'), signature: 'totally-bogus')
      expect(last_response.status).to eq(401)
    end

    it 'rejects a signature made with the wrong secret' do
      post_webhook(JSON.generate('event' => 'link.clicked'), secret: 'whsec_attacker')
      expect(last_response.status).to eq(401)
    end

    it 'rejects a body tampered after signing' do
      body = JSON.generate('event' => 'link.clicked')
      header = sign_body(body)
      post(webhook_path, "#{body} ",
           'CONTENT_TYPE' => 'application/json', 'HTTP_X_REROUT_SIGNATURE' => header)
      expect(last_response.status).to eq(401)
    end

    it 'rejects a stale signature outside the tolerance window' do
      old = Time.now.to_i - 3600
      post_webhook(JSON.generate('event' => 'link.clicked'), timestamp: old)
      expect(last_response.status).to eq(401)
    end

    it 'returns a JSON error body on 401' do
      post_webhook(JSON.generate('event' => 'link.clicked'), signature: 'garbage')
      expect(JSON.parse(last_response.body)).to eq('error' => 'invalid signature')
    end

    it 'does not dispatch a notification on a bad signature' do
      body = JSON.generate('event' => 'link.clicked')
      events = capture_notifications('rerout.webhook') do
        post_webhook(body, signature: 'garbage')
      end
      expect(events).to be_empty
    end
  end

  describe '400 — bad body' do
    it 'rejects a non-JSON body' do
      post_webhook('this is not json')
      expect(last_response.status).to eq(400)
    end

    it 'rejects a JSON array body' do
      post_webhook(JSON.generate(%w[not an object]))
      expect(last_response.status).to eq(400)
    end

    it 'rejects a JSON scalar body' do
      post_webhook(JSON.generate('just a string'))
      expect(last_response.status).to eq(400)
    end

    it 'rejects an empty body' do
      post_webhook('')
      expect(last_response.status).to eq(400)
    end

    it 'returns a JSON error body on 400' do
      post_webhook('not json')
      expect(JSON.parse(last_response.body)).to eq('error' => 'body must be a JSON object')
    end
  end

  describe 'configuration' do
    it 'accepts an old timestamp when tolerance is 0' do
      Rerout::Rails.configure { |c| c.signature_tolerance_seconds = 0 }
      old = Time.now.to_i - 100_000
      post_webhook(JSON.generate('event' => 'link.clicked'), timestamp: old)
      expect(last_response.status).to eq(200)
    end

    it 'fails the request when the webhook secret is unset' do
      Rerout::Rails.configure { |c| c.webhook_secret = nil }
      post_webhook(JSON.generate('event' => 'link.clicked'))
      # The ConfigurationError raised inside the action surfaces as a 5xx —
      # the delivery is never treated as verified.
      expect(last_response.status).to eq(500)
    end
  end

  describe 'HTTP method handling' do
    it 'does not route GET to the webhook receiver' do
      get webhook_path
      expect(last_response.status).to eq(404)
    end
  end
end
