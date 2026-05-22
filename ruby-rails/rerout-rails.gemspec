# frozen_string_literal: true

require_relative 'lib/rerout/rails/version'

Gem::Specification.new do |spec|
  spec.name        = 'rerout-rails'
  spec.version     = Rerout::Rails::VERSION
  spec.authors     = ['Codecraft Solutions']
  spec.email       = ['hello@codecraftsolutions.co.za']

  spec.summary     = 'Official Rails integration for the Rerout branded-link API.'
  spec.description = <<~DESC
    Rails integration for the Rerout API. Wraps the `rerout` gem with a
    cached, initializer-driven client, an install generator, and a webhook
    controller that verifies signatures and dispatches each delivery through
    ActiveSupport::Notifications.
  DESC
  spec.homepage    = 'https://github.com/ModestNerds-Co/rerout-sdks'
  spec.license     = 'MIT'

  spec.required_ruby_version = '>= 3.0.0'

  spec.metadata = {
    'homepage_uri' => spec.homepage,
    'source_code_uri' => 'https://github.com/ModestNerds-Co/rerout-sdks/tree/main/ruby-rails',
    'changelog_uri' => 'https://github.com/ModestNerds-Co/rerout-sdks/blob/main/ruby-rails/CHANGELOG.md',
    'bug_tracker_uri' => 'https://github.com/ModestNerds-Co/rerout-sdks/issues',
    'documentation_uri' => 'https://rerout.co/docs',
    'rubygems_mfa_required' => 'true'
  }

  spec.files = Dir.chdir(__dir__) do
    Dir[
      'lib/**/*.rb',
      'lib/generators/**/templates/*',
      'README.md',
      'CHANGELOG.md',
      'LICENSE'
    ]
  end
  spec.require_paths = ['lib']

  spec.add_dependency 'railties', '>= 7.0', '< 9.0'
  spec.add_dependency 'rerout', '~> 0.1'

  spec.add_development_dependency 'actionpack', '>= 7.0', '< 9.0'
  spec.add_development_dependency 'rack-test', '~> 2.1'
  spec.add_development_dependency 'rake', '~> 13.0'
  spec.add_development_dependency 'rspec', '~> 3.12'
  spec.add_development_dependency 'rubocop', '~> 1.60'
  spec.add_development_dependency 'rubocop-performance', '~> 1.20'
  spec.add_development_dependency 'rubocop-rspec', '~> 3.0'
end
