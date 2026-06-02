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

    # A short link.
    class Link
      ATTRS = %i[
        code short_url domain_hostname target_url project_id expires_at
        is_active seo_title seo_description seo_image_url seo_canonical_url
        seo_noindex seo_updated_at tags created_at updated_at
      ].freeze

      attr_reader(*ATTRS)

      def initialize(**attrs)
        ATTRS.each { |k| instance_variable_set(:"@#{k}", attrs[k]) }
        @tags = (@tags || []).freeze
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
          updated_at: hash['updated_at']
        )
      end

      def to_h
        ATTRS.to_h do |k|
          [k, k == :tags ? tags.map(&:to_h) : public_send(k)]
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
  end
end
