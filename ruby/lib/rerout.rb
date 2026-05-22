# frozen_string_literal: true

require_relative 'rerout/version'
require_relative 'rerout/error'
require_relative 'rerout/models'
require_relative 'rerout/create_link_input'
require_relative 'rerout/update_link_input'
require_relative 'rerout/qr_options'
require_relative 'rerout/webhooks'
require_relative 'rerout/links'
require_relative 'rerout/project'
require_relative 'rerout/qr'
require_relative 'rerout/client'

# Official Ruby SDK for the Rerout branded-link API.
#
# Branded link infrastructure on Cloudflare — create short links, render QR
# codes, read analytics, and verify webhook signatures.
#
# @example Hello world
#   require 'rerout'
#
#   rerout = Rerout::Client.new(api_key: ENV.fetch('REROUT_API_KEY'))
#   link = rerout.links.create(
#     Rerout::CreateLinkInput.new(target_url: 'https://example.com/sale')
#   )
#   puts link.short_url
#
# @see https://rerout.co
# @see https://github.com/ModestNerds-Co/rerout-sdks
module Rerout
end
