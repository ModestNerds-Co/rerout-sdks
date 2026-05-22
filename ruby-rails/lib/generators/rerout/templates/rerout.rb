# frozen_string_literal: true

# Rerout configuration.
#
# Keep real credentials out of source control — prefer Rails encrypted
# credentials (`Rails.application.credentials`) or environment variables.
#
# Docs: https://rerout.co/docs
Rerout::Rails.configure do |config|
  # Project API key (`rrk_…`). Required to use `Rerout::Rails.client`.
  config.api_key = ENV.fetch('REROUT_API_KEY', nil)

  # Endpoint signing secret (`whsec_…`). Required for webhook verification.
  config.webhook_secret = ENV.fetch('REROUT_WEBHOOK_SECRET', nil)

  # Override the API base URL (staging / self-hosted). Optional.
  # config.base_url = 'https://api.rerout.co'

  # Per-request timeout in seconds. Optional, defaults to 30.
  # config.timeout = 30

  # Webhook signature timestamp tolerance in seconds. `0` disables the
  # staleness check. Optional, defaults to 300.
  # config.signature_tolerance_seconds = 300
end

# React to verified webhook deliveries via ActiveSupport::Notifications.
# `rerout.webhook` fires for every delivery; per-event topics
# (`rerout.link.clicked`, `rerout.qr.scanned`, …) fire for known events.
#
# ActiveSupport::Notifications.subscribe('rerout.link.clicked') do |event|
#   code = event.payload[:body]['code']
#   Rails.logger.info("Rerout: link #{code} clicked")
# end
