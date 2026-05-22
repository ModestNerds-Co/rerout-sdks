# frozen_string_literal: true

require 'spec_helper'
require 'fileutils'
require 'tmpdir'
require 'rails/generators'
require 'generators/rerout/install_generator'

RSpec.describe Rerout::Generators::InstallGenerator do
  let(:destination) { Dir.mktmpdir('rerout-rails-gen') }

  before do
    # A routes.rb the generator can splice the webhook route into.
    FileUtils.mkdir_p(File.join(destination, 'config'))
    File.write(
      File.join(destination, 'config', 'routes.rb'),
      "Rails.application.routes.draw do\nend\n"
    )
  end

  after { FileUtils.remove_entry(destination) if File.directory?(destination) }

  def run_generator
    described_class.start([], destination_root: destination, behavior: :invoke)
  end

  def read(relative)
    File.read(File.join(destination, relative))
  end

  it 'creates the initializer file' do
    run_generator
    expect(File).to exist(File.join(destination, 'config/initializers/rerout.rb'))
  end

  it 'writes a Rerout::Rails.configure block into the initializer' do
    run_generator
    expect(read('config/initializers/rerout.rb')).to include('Rerout::Rails.configure')
  end

  it 'references the API key env var in the initializer' do
    run_generator
    expect(read('config/initializers/rerout.rb')).to include('REROUT_API_KEY')
  end

  it 'references the webhook secret env var in the initializer' do
    run_generator
    expect(read('config/initializers/rerout.rb')).to include('REROUT_WEBHOOK_SECRET')
  end

  it 'mounts the webhook route in config/routes.rb' do
    run_generator
    routes = read('config/routes.rb')
    expect(routes).to include('rerout/rails/webhook#receive')
    expect(routes).to include('/rerout/webhooks')
  end

  it 'names the webhook route' do
    run_generator
    expect(read('config/routes.rb')).to include('as: :rerout_webhook')
  end
end
