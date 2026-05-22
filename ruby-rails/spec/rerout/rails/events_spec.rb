# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Rails::Events do
  describe '.dispatch' do
    it 'instruments the catch-all topic for every delivery' do
      events = capture_notifications('rerout.webhook') do
        described_class.dispatch(event: 'link.clicked', body: { 'code' => 'q4' })
      end
      expect(events.size).to eq(1)
      expect(events.first[:event]).to eq('link.clicked')
      expect(events.first[:body]).to eq('code' => 'q4')
    end

    it 'instruments the catch-all even for an unknown event' do
      events = capture_notifications('rerout.webhook') do
        described_class.dispatch(event: 'something.new', body: {})
      end
      expect(events.size).to eq(1)
    end

    it 'instruments the event-specific topic for a known event' do
      events = capture_notifications('rerout.link.clicked') do
        described_class.dispatch(event: 'link.clicked', body: { 'code' => 'q4' })
      end
      expect(events.size).to eq(1)
      expect(events.first[:body]).to eq('code' => 'q4')
    end

    it 'does not fire any event-specific topic for an unknown event' do
      events = capture_notifications('rerout.something.new') do
        described_class.dispatch(event: 'something.new', body: {})
      end
      expect(events).to be_empty
    end

    it 'maps link.created to rerout.link.created' do
      expect_topic('link.created', 'rerout.link.created')
    end

    it 'maps link.updated to rerout.link.updated' do
      expect_topic('link.updated', 'rerout.link.updated')
    end

    it 'maps link.deleted to rerout.link.deleted' do
      expect_topic('link.deleted', 'rerout.link.deleted')
    end

    it 'maps qr.scanned to rerout.qr.scanned' do
      expect_topic('qr.scanned', 'rerout.qr.scanned')
    end

    it 'carries the request object in the payload' do
      sentinel = Object.new
      events = capture_notifications('rerout.webhook') do
        described_class.dispatch(event: '', body: {}, request: sentinel)
      end
      expect(events.first[:request]).to be(sentinel)
    end
  end

  describe '.all_topics' do
    it 'lists the catch-all plus every specific topic' do
      expect(described_class.all_topics)
        .to contain_exactly('rerout.webhook', 'rerout.link.created',
                            'rerout.link.updated', 'rerout.link.deleted',
                            'rerout.link.clicked', 'rerout.qr.scanned')
    end
  end

  def expect_topic(event, topic)
    events = capture_notifications(topic) do
      described_class.dispatch(event: event, body: { 'ok' => true })
    end
    expect(events.size).to eq(1)
  end
end
