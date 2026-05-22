<?php

declare(strict_types=1);

namespace Rerout\Laravel\Tests;

use Illuminate\Contracts\Config\Repository as ConfigRepository;
use Illuminate\Contracts\Foundation\Application as ApplicationContract;
use Illuminate\Foundation\Application;
use Orchestra\Testbench\TestCase as Orchestra;
use Rerout\Laravel\ReroutServiceProvider;

/**
 * Base test case for the Rerout Laravel integration. Boots a minimal Laravel
 * application via Orchestra Testbench with the Rerout service provider
 * registered.
 */
abstract class TestCase extends Orchestra
{
    /**
     * The booted application container, guaranteed non-null. Use this instead
     * of the nullable `$this->app` property in tests.
     */
    protected function container(): Application
    {
        $app = $this->app;
        self::assertInstanceOf(Application::class, $app, 'The test application has not been booted.');

        return $app;
    }

    /**
     * The application's config repository.
     */
    protected function config(): ConfigRepository
    {
        $config = $this->container()->make('config');
        self::assertInstanceOf(ConfigRepository::class, $config);

        return $config;
    }

    /**
     * Run an Artisan command and return its pending-command handle for
     * chained expectations.
     *
     * @param array<string, mixed> $parameters
     */
    protected function pendingArtisan(string $command, array $parameters = []): \Illuminate\Testing\PendingCommand
    {
        $pending = $this->artisan($command, $parameters);
        self::assertInstanceOf(\Illuminate\Testing\PendingCommand::class, $pending);

        return $pending;
    }

    /**
     * Register the package's service provider with the test application.
     *
     * @param ApplicationContract $app
     *
     * @return array<int, class-string>
     */
    protected function getPackageProviders($app): array
    {
        return [ReroutServiceProvider::class];
    }

    /**
     * Register the package's facade aliases with the test application.
     *
     * @param ApplicationContract $app
     *
     * @return array<string, class-string>
     */
    protected function getPackageAliases($app): array
    {
        return ['Rerout' => \Rerout\Laravel\Facades\Rerout::class];
    }

    /**
     * Provide a sane default environment for every test.
     *
     * @param ApplicationContract $app
     */
    protected function defineEnvironment($app): void
    {
        $config = $app->make('config');
        if ($config instanceof ConfigRepository) {
            $config->set('rerout.api_key', 'rrk_test_key');
            $config->set('rerout.webhook.secret', 'whsec_test_secret');
        }
    }
}
