# frozen_string_literal: true

module Rerout
  module Rails
    # Maps Rerout webhook event names onto `ActiveSupport::Notifications` topic
    # names and dispatches verified deliveries.
    #
    # Every verified webhook is instrumented under {CATCH_ALL} (`rerout.webhook`)
    # so a single subscriber can see everything. Events with a recognised
    # `event` field are *also* instrumented under a dedicated, more specific
    # topic so subscribers can listen narrowly.
    #
    # Subscribe the Rails way:
    #
    #   ActiveSupport::Notifications.subscribe('rerout.link.clicked') do |event|
    #     code = event.payload[:body]['code']
    #     Rails.logger.info("link #{code} clicked")
    #   end
    #
    # The instrumentation payload carries:
    #
    # - `:event`   — the event-type string (e.g. `"link.clicked"`), or `""`.
    # - `:body`    — the full parsed JSON body as a Hash.
    # - `:request` — the `ActionDispatch::Request` that delivered the webhook.
    module Events
      # Topic every verified webhook is instrumented under.
      CATCH_ALL = 'rerout.webhook'

      # Known `event` strings mapped onto their dedicated notification topic.
      # Events absent from this map still fire {CATCH_ALL}.
      TOPICS = {
        'link.created' => 'rerout.link.created',
        'link.updated' => 'rerout.link.updated',
        'link.deleted' => 'rerout.link.deleted',
        'link.clicked' => 'rerout.link.clicked',
        'qr.scanned' => 'rerout.qr.scanned'
      }.freeze

      module_function

      # Instrument a verified webhook delivery.
      #
      # Fires {CATCH_ALL} unconditionally, then the event-specific topic when
      # `event` is one of {TOPICS}.
      #
      # @param event [String] the event-type string; may be empty.
      # @param body [Hash] the parsed JSON body.
      # @param request [ActionDispatch::Request, nil] the delivering request.
      # @return [void]
      def dispatch(event:, body:, request: nil)
        payload = { event: event, body: body, request: request }

        ActiveSupport::Notifications.instrument(CATCH_ALL, payload)

        specific = TOPICS[event]
        ActiveSupport::Notifications.instrument(specific, payload) if specific
      end

      # @return [Array<String>] every notification topic this gem can emit.
      def all_topics
        [CATCH_ALL, *TOPICS.values]
      end
    end
  end
end
