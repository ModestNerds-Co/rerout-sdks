# frozen_string_literal: true

module Rerout
  # Plain-old Ruby value objects returned by the Rerout API. Each model is a
  # frozen struct-like class with `from_hash` for JSON ingestion and value
  # equality semantics.
  module Models
    # A label attached to a link — `{ id:, name:, color: }`. Read-only; the API
    # ignores tag writes for API-key clients.
    class Tag
      attr_reader :id, :name, :color

      def initialize(id:, name:, color:)
        @id = id
        @name = name
        @color = color
        freeze
      end

      def self.from_hash(hash)
        new(id: hash['id'], name: hash['name'], color: hash['color'])
      end

      def to_h
        { id: id, name: name, color: color }
      end

      def ==(other)
        other.is_a?(Tag) && other.id == id && other.name == name && other.color == color
      end
      alias eql? ==

      def hash
        [self.class, id, name, color].hash
      end
    end

    # A tag with the number of live (non-deleted) links it is attached to —
    # `{ id:, name:, color:, link_count: }`. Returned by `tags.list`; the
    # create/update responses use the plain {Tag} (no `link_count`).
    class TagSummary
      attr_reader :id, :name, :color, :link_count

      def initialize(id:, name:, color:, link_count:)
        @id = id
        @name = name
        @color = color
        @link_count = link_count
        freeze
      end

      def self.from_hash(hash)
        new(
          id: hash['id'],
          name: hash['name'],
          color: hash['color'],
          link_count: hash.fetch('link_count', 0)
        )
      end

      def to_h
        { id: id, name: name, color: color, link_count: link_count }
      end

      def ==(other)
        other.is_a?(TagSummary) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        [self.class, id, name, color, link_count].hash
      end
    end

    # Result of `tags.list` — the project's tags with their live link counts.
    class ListTagsResult
      attr_reader :tags

      def initialize(tags:)
        @tags = tags.freeze
        freeze
      end

      def self.from_hash(hash)
        new(tags: (hash['tags'] || []).map { |t| TagSummary.from_hash(t) })
      end

      def to_h
        { tags: tags.map(&:to_h) }
      end

      def ==(other)
        other.is_a?(ListTagsResult) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # A Smart Link routing rule — send matching visitors to `target_url`.
    # `condition_type` is `"country"` or `"device"`; `condition_op` is `"is"`,
    # `"is_not"`, or `"in"`; `condition_value` is the value to compare against
    # (e.g. `"US"` or `"US,CA,GB"` for `"in"`).
    class RoutingRule
      attr_reader :condition_type, :condition_op, :condition_value, :target_url

      def initialize(condition_type:, condition_op:, condition_value:, target_url:)
        @condition_type = condition_type
        @condition_op = condition_op
        @condition_value = condition_value
        @target_url = target_url
        freeze
      end

      def self.from_hash(hash)
        new(
          condition_type: hash['condition_type'],
          condition_op: hash['condition_op'],
          condition_value: hash['condition_value'],
          target_url: hash['target_url']
        )
      end

      def to_h
        {
          'condition_type' => condition_type,
          'condition_op' => condition_op,
          'condition_value' => condition_value,
          'target_url' => target_url
        }
      end

      def ==(other)
        other.is_a?(RoutingRule) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # One A/B-test variant attached to a Smart Link. `id` is assigned
    # server-side (read-only); `weight` controls the relative traffic share and
    # defaults to `1`.
    class AbVariant
      attr_reader :id, :target_url, :weight

      def initialize(target_url:, weight: 1, id: nil)
        @id = id
        @target_url = target_url
        @weight = weight
        freeze
      end

      def self.from_hash(hash)
        new(
          id: hash['id'],
          target_url: hash['target_url'],
          weight: hash.fetch('weight', 1)
        )
      end

      # Render for create/update. The server-assigned `id` is never sent.
      def to_h
        { 'target_url' => target_url, 'weight' => weight }
      end

      def ==(other)
        other.is_a?(AbVariant) && other.id == id &&
          other.target_url == target_url && other.weight == weight
      end
      alias eql? ==

      def hash
        [self.class, id, target_url, weight].hash
      end
    end

    # A short link.
    class Link
      ATTRS = %i[
        code short_url domain_hostname target_url project_id expires_at
        is_active seo_title seo_description seo_image_url seo_canonical_url
        seo_noindex seo_updated_at tags created_at updated_at
        password_protected max_clicks click_count track_conversions
        routing_rules ab_variants
      ].freeze

      attr_reader(*ATTRS)

      def initialize(**attrs)
        ATTRS.each { |k| instance_variable_set(:"@#{k}", attrs[k]) }
        @tags = (@tags || []).freeze
        @routing_rules = (@routing_rules || []).freeze
        @ab_variants = (@ab_variants || []).freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          code: hash['code'],
          short_url: hash['short_url'],
          domain_hostname: hash['domain_hostname'],
          target_url: hash['target_url'],
          project_id: hash['project_id'],
          expires_at: hash['expires_at'],
          is_active: hash['is_active'],
          seo_title: hash['seo_title'],
          seo_description: hash['seo_description'],
          seo_image_url: hash['seo_image_url'],
          seo_canonical_url: hash['seo_canonical_url'],
          seo_noindex: hash.fetch('seo_noindex', true),
          seo_updated_at: hash['seo_updated_at'],
          tags: (hash['tags'] || []).map { |t| Tag.from_hash(t) },
          created_at: hash['created_at'],
          updated_at: hash['updated_at'],
          password_protected: hash.fetch('password_protected', false),
          max_clicks: hash['max_clicks'],
          click_count: hash.fetch('click_count', 0),
          track_conversions: hash.fetch('track_conversions', false),
          routing_rules: (hash['routing_rules'] || []).map { |r| RoutingRule.from_hash(r) },
          ab_variants: (hash['ab_variants'] || []).map { |v| AbVariant.from_hash(v) }
        )
      end

      def to_h
        ATTRS.to_h do |k|
          [k, case k
              when :tags then tags.map(&:to_h)
              when :routing_rules then routing_rules.map(&:to_h)
              when :ab_variants then ab_variants.map(&:to_h)
              else public_send(k)
              end]
        end
      end

      def ==(other)
        other.is_a?(Link) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # A breakdown bucket — `{ value: "ZA", clicks: 42 }`.
    class StatsBreakdown
      attr_reader :value, :clicks

      def initialize(value:, clicks:)
        @value = value
        @clicks = clicks
        freeze
      end

      def self.from_hash(hash)
        new(value: hash['value'], clicks: hash['clicks'])
      end

      def to_h
        { value: value, clicks: clicks }
      end

      def ==(other)
        other.is_a?(StatsBreakdown) && other.value == value && other.clicks == clicks
      end
      alias eql? ==

      def hash
        [self.class, value, clicks].hash
      end
    end

    # One day in a `daily` time-series.
    class DailyClicksPoint
      attr_reader :day, :clicks, :qr_scans

      def initialize(day:, clicks:, qr_scans:)
        @day = day
        @clicks = clicks
        @qr_scans = qr_scans
        freeze
      end

      def self.from_hash(hash)
        new(day: hash['day'], clicks: hash['clicks'], qr_scans: hash['qr_scans'])
      end

      def to_h
        { day: day, clicks: clicks, qr_scans: qr_scans }
      end

      def ==(other)
        other.is_a?(DailyClicksPoint) && other.day == day &&
          other.clicks == clicks && other.qr_scans == qr_scans
      end
      alias eql? ==

      def hash
        [self.class, day, clicks, qr_scans].hash
      end
    end

    # Per-link click stats response.
    class LinkStats
      attr_reader :code, :days, :total_clicks, :qr_scans, :countries, :referrers

      def initialize(code:, days:, total_clicks:, qr_scans:, countries:, referrers:)
        @code = code
        @days = days
        @total_clicks = total_clicks
        @qr_scans = qr_scans
        @countries = countries.freeze
        @referrers = referrers.freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          code: hash['code'],
          days: hash['days'],
          total_clicks: hash['total_clicks'],
          qr_scans: hash['qr_scans'],
          countries: (hash['countries'] || []).map { |c| StatsBreakdown.from_hash(c) },
          referrers: (hash['referrers'] || []).map { |c| StatsBreakdown.from_hash(c) }
        )
      end

      def to_h
        {
          code: code, days: days, total_clicks: total_clicks, qr_scans: qr_scans,
          countries: countries.map(&:to_h), referrers: referrers.map(&:to_h)
        }
      end

      def ==(other)
        other.is_a?(LinkStats) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Project-wide aggregate stats response.
    class ProjectStats
      attr_reader :days, :total_clicks, :qr_scans, :daily, :countries, :referrers,
                  :devices, :browsers, :top_codes

      def initialize(**attrs)
        @days = attrs[:days]
        @total_clicks = attrs[:total_clicks]
        @qr_scans = attrs[:qr_scans]
        @daily = attrs[:daily].freeze
        @countries = attrs[:countries].freeze
        @referrers = attrs[:referrers].freeze
        @devices = attrs[:devices].freeze
        @browsers = attrs[:browsers].freeze
        @top_codes = attrs[:top_codes].freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          days: hash['days'],
          total_clicks: hash['total_clicks'],
          qr_scans: hash['qr_scans'],
          daily: (hash['daily'] || []).map { |d| DailyClicksPoint.from_hash(d) },
          countries: (hash['countries'] || []).map { |b| StatsBreakdown.from_hash(b) },
          referrers: (hash['referrers'] || []).map { |b| StatsBreakdown.from_hash(b) },
          devices: (hash['devices'] || []).map { |b| StatsBreakdown.from_hash(b) },
          browsers: (hash['browsers'] || []).map { |b| StatsBreakdown.from_hash(b) },
          top_codes: (hash['top_codes'] || []).map { |b| StatsBreakdown.from_hash(b) }
        )
      end

      def to_h
        {
          days: days, total_clicks: total_clicks, qr_scans: qr_scans,
          daily: daily.map(&:to_h),
          countries: countries.map(&:to_h),
          referrers: referrers.map(&:to_h),
          devices: devices.map(&:to_h),
          browsers: browsers.map(&:to_h),
          top_codes: top_codes.map(&:to_h)
        }
      end

      def ==(other)
        other.is_a?(ProjectStats) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Paginated list of links.
    class ListLinksResult
      attr_reader :links, :next_cursor

      def initialize(links:, next_cursor:)
        @links = links.freeze
        @next_cursor = next_cursor
        freeze
      end

      def self.from_hash(hash)
        new(
          links: (hash['links'] || []).map { |l| Link.from_hash(l) },
          next_cursor: hash['next_cursor']
        )
      end

      def to_h
        { links: links.map(&:to_h), next_cursor: next_cursor }
      end

      def ==(other)
        other.is_a?(ListLinksResult) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Project identity envelope.
    class Project
      attr_reader :id, :name, :slug

      def initialize(id:, name:, slug:)
        @id = id
        @name = name
        @slug = slug
        freeze
      end

      def self.from_hash(hash)
        new(id: hash['id'], name: hash['name'], slug: hash['slug'])
      end

      def to_h
        { id: id, name: name, slug: slug }
      end

      def ==(other)
        other.is_a?(Project) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        [self.class, id, name, slug].hash
      end
    end

    # A webhook endpoint registered to the project. Mirrors the server-side
    # `WebhookEndpointResponse`.
    class Webhook
      ATTRS = %i[
        id project_id name url events is_active payload_format
        created_at updated_at last_delivery_at last_success_at last_failure_at
      ].freeze

      attr_reader(*ATTRS)

      def initialize(**attrs)
        ATTRS.each { |k| instance_variable_set(:"@#{k}", attrs[k]) }
        @events = (@events || []).freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          id: hash['id'],
          project_id: hash['project_id'],
          name: hash['name'],
          url: hash['url'],
          events: hash['events'] || [],
          is_active: hash['is_active'],
          payload_format: hash['payload_format'],
          created_at: hash['created_at'],
          updated_at: hash['updated_at'],
          last_delivery_at: hash['last_delivery_at'],
          last_success_at: hash['last_success_at'],
          last_failure_at: hash['last_failure_at']
        )
      end

      def to_h
        ATTRS.to_h { |k| [k, public_send(k)] }
      end

      def ==(other)
        other.is_a?(Webhook) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Result of creating a webhook. The `signing_secret` (`whsec_…`) is
    # returned **once** — store it now; it is never shown again.
    class CreatedWebhook
      attr_reader :endpoint, :signing_secret

      def initialize(endpoint:, signing_secret:)
        @endpoint = endpoint
        @signing_secret = signing_secret
        freeze
      end

      def self.from_hash(hash)
        new(
          endpoint: Webhook.from_hash(hash['endpoint'] || {}),
          signing_secret: hash['signing_secret']
        )
      end

      def to_h
        { endpoint: endpoint.to_h, signing_secret: signing_secret }
      end

      def ==(other)
        other.is_a?(CreatedWebhook) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # List of webhook endpoints plus every event type the server can deliver.
    class ListWebhooksResult
      attr_reader :endpoints, :event_types

      def initialize(endpoints:, event_types:)
        @endpoints = endpoints.freeze
        @event_types = event_types.freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          endpoints: (hash['endpoints'] || []).map { |e| Webhook.from_hash(e) },
          event_types: hash['event_types'] || []
        )
      end

      def to_h
        { endpoints: endpoints.map(&:to_h), event_types: event_types }
      end

      def ==(other)
        other.is_a?(ListWebhooksResult) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Result of recording a conversion via `POST /v1/conversions`. `recorded`
    # is true when stored; `duplicate` is true when an identical conversion for
    # the same click had already been recorded (the call is idempotent).
    class RecordedConversion
      attr_reader :recorded, :duplicate

      def initialize(recorded:, duplicate:)
        @recorded = recorded
        @duplicate = duplicate
        freeze
      end

      def self.from_hash(hash)
        new(
          recorded: hash.fetch('recorded', false),
          duplicate: hash.fetch('duplicate', false)
        )
      end

      def to_h
        { recorded: recorded, duplicate: duplicate }
      end

      def ==(other)
        other.is_a?(RecordedConversion) && other.recorded == recorded && other.duplicate == duplicate
      end
      alias eql? ==

      def hash
        [self.class, recorded, duplicate].hash
      end
    end

    # Outcome of one link in a `links.create_batch` call. `index` is the input
    # position; `ok` is true on success (then `code` is set), false on failure
    # (then `error` carries the reason).
    class BatchLinkResult
      attr_reader :index, :ok, :code, :error

      def initialize(index:, ok:, code: nil, error: nil)
        @index = index
        @ok = ok
        @code = code
        @error = error
        freeze
      end

      def self.from_hash(hash)
        new(
          index: hash['index'],
          ok: hash['ok'],
          code: hash['code'],
          error: hash['error']
        )
      end

      def to_h
        { index: index, ok: ok, code: code, error: error }
      end

      def ==(other)
        other.is_a?(BatchLinkResult) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end

    # Aggregate result of `links.create_batch` (`POST /v1/links/batch`).
    class BatchCreateLinksResult
      attr_reader :created, :total, :results

      def initialize(created:, total:, results:)
        @created = created
        @total = total
        @results = results.freeze
        freeze
      end

      def self.from_hash(hash)
        new(
          created: hash.fetch('created', 0),
          total: hash.fetch('total', 0),
          results: (hash['results'] || []).map { |r| BatchLinkResult.from_hash(r) }
        )
      end

      def to_h
        { created: created, total: total, results: results.map(&:to_h) }
      end

      def ==(other)
        other.is_a?(BatchCreateLinksResult) && other.to_h == to_h
      end
      alias eql? ==

      def hash
        to_h.hash
      end
    end
  end
end
