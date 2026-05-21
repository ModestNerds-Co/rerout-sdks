# frozen_string_literal: true

require_relative 'lib/rerout/version'

Gem::Specification.new do |spec|
  spec.name        = 'rerout'
  spec.version     = Rerout::VERSION
  spec.authors     = ['Codecraft Solutions']
  spec.email       = ['hello@codecraftsolutions.co.za']

  spec.summary     = 'Official Ruby SDK for the Rerout branded-link API.'
  spec.description = <<~DESC
    Ruby client for the Rerout API — create short links, render QR codes,
    read analytics, and verify webhook signatures. Built on Faraday with
    an injectable connection for tests and edge deployments.
  DESC
  spec.homepage    = 'https://github.com/ModestNerds-Co/rerout-sdks'
  spec.license     = 'MIT'

  spec.required_ruby_version = '>= 3.0.0'

  spec.metadata = {
    'homepage_uri'      => spec.homepage,
    'source_code_uri'   => 'https://github.com/ModestNerds-Co/rerout-sdks/tree/main/ruby',
    'changelog_uri'     => 'https://github.com/ModestNerds-Co/rerout-sdks/blob/main/ruby/CHANGELOG.md',
    'bug_tracker_uri'   => 'https://github.com/ModestNerds-Co/rerout-sdks/issues',
    'documentation_uri' => 'https://rerout.co/docs',
    'rubygems_mfa_required' => 'true'
  }

  spec.files = Dir.chdir(__dir__) do
    Dir[
      'lib/**/*.rb',
      'README.md',
      'CHANGELOG.md',
      'LICENSE'
    ]
  end
  spec.require_paths = ['lib']

  spec.add_dependency 'faraday', '~> 2.0'

  spec.add_development_dependency 'faraday-net_http', '~> 3.0'
  spec.add_development_dependency 'rake', '~> 13.0'
  spec.add_development_dependency 'rspec', '~> 3.12'
  spec.add_development_dependency 'rubocop', '~> 1.60'
  spec.add_development_dependency 'rubocop-performance', '~> 1.20'
  spec.add_development_dependency 'rubocop-rspec', '~> 3.0'
  spec.add_development_dependency 'webmock', '~> 3.20'
end
