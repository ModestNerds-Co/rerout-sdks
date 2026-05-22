<?php

declare(strict_types=1);

namespace Rerout\Laravel;

use Illuminate\Contracts\Config\Repository as ConfigRepository;
use Illuminate\Support\ServiceProvider;
use Rerout\Laravel\Console\PingCommand;
use Rerout\Rerout as ReroutClient;

/**
 * Wires the Rerout SDK into a Laravel application.
 *
 * Registers:
 *
 * - the `config/rerout.php` config file (publishable + merged),
 * - a shared {@see ReroutClient} singleton resolvable by class name or via the
 *   `Rerout` facade,
 * - the `rerout:ping` Artisan command,
 * - and — when `rerout.webhook.route.enabled` is true — a POST route running
 *   the bundled {@see \Rerout\Laravel\Http\WebhookController}.
 *
 * The client reads `REROUT_API_KEY`, `REROUT_BASE_URL`, and `REROUT_TIMEOUT`
 * from the environment via the config file.
 */
final class ReroutServiceProvider extends ServiceProvider
{
    /**
     * Register container bindings. Runs before `boot()`.
     */
    public function register(): void
    {
        $this->mergeConfigFrom(__DIR__ . '/../config/rerout.php', 'rerout');

        $this->app->singleton(ReroutClient::class, static function ($app): ReroutClient {
            /** @var ConfigRepository $config */
            $config = $app->make('config');

            $apiKey = $config->get('rerout.api_key');
            $apiKey = is_string($apiKey) ? $apiKey : '';

            /** @var array{base_url?: string, timeout?: int} $options */
            $options = [];

            $baseUrl = $config->get('rerout.base_url');
            if (is_string($baseUrl) && $baseUrl !== '') {
                $options['base_url'] = $baseUrl;
            }

            $timeout = $config->get('rerout.timeout');
            if (is_int($timeout) && $timeout > 0) {
                $options['timeout'] = $timeout;
            }

            return new ReroutClient($apiKey, $options);
        });

        $this->app->alias(ReroutClient::class, 'rerout');
    }

    /**
     * Bootstrap publishing, routes, and console commands.
     */
    public function boot(): void
    {
        if ($this->app->runningInConsole()) {
            $this->publishes([
                __DIR__ . '/../config/rerout.php' => $this->app->configPath('rerout.php'),
            ], 'rerout-config');

            $this->commands([
                PingCommand::class,
            ]);
        }

        $this->registerWebhookRoute();
    }

    /**
     * Services provided by this provider, for the deferred-provider manifest.
     *
     * @return array<int, string>
     */
    public function provides(): array
    {
        return [ReroutClient::class, 'rerout'];
    }

    /**
     * Register the bundled webhook route when enabled in config.
     */
    private function registerWebhookRoute(): void
    {
        /** @var ConfigRepository $config */
        $config = $this->app->make('config');

        if ($config->get('rerout.webhook.route.enabled') !== true) {
            return;
        }

        $this->loadRoutesFrom(__DIR__ . '/../routes/web.php');
    }
}
