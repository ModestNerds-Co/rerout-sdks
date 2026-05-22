<?php

declare(strict_types=1);

namespace Rerout\Laravel\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Laravel\Facades\Rerout as ReroutFacade;
use Rerout\Laravel\ReroutServiceProvider;
use Rerout\Rerout as ReroutClient;

/**
 * Verifies the service provider registers config and the client singleton,
 * and that environment variables flow through to the bound client.
 */
#[CoversClass(ReroutServiceProvider::class)]
#[CoversClass(ReroutFacade::class)]
final class ServiceProviderTest extends TestCase
{
    public function testConfigIsMerged(): void
    {
        self::assertSame('rrk_test_key', $this->config()->get('rerout.api_key'));
        self::assertSame('whsec_test_secret', $this->config()->get('rerout.webhook.secret'));
        self::assertSame(300, $this->config()->get('rerout.webhook.tolerance'));
    }

    public function testClientIsBoundAsASingleton(): void
    {
        $first = $this->container()->make(ReroutClient::class);
        $second = $this->container()->make(ReroutClient::class);

        self::assertInstanceOf(ReroutClient::class, $first);
        self::assertSame($first, $second);
    }

    public function testClientIsResolvableByTheReroutAlias(): void
    {
        $byAlias = $this->container()->make('rerout');
        $byClass = $this->container()->make(ReroutClient::class);

        self::assertInstanceOf(ReroutClient::class, $byAlias);
        self::assertSame($byAlias, $byClass);
    }

    public function testFacadeResolvesToTheClient(): void
    {
        self::assertInstanceOf(\Rerout\Resources\Links::class, ReroutFacade::links());
        self::assertInstanceOf(\Rerout\Resources\Project::class, ReroutFacade::project());
        self::assertInstanceOf(\Rerout\Resources\Qr::class, ReroutFacade::qr());
    }

    public function testBaseUrlDefaultsToProductionWhenUnset(): void
    {
        $client = $this->container()->make(ReroutClient::class);
        self::assertInstanceOf(ReroutClient::class, $client);

        self::assertSame('https://api.rerout.co', $client->baseUrl());
    }

    public function testBaseUrlIsReadFromConfig(): void
    {
        $this->config()->set('rerout.base_url', 'https://staging.rerout.co');
        $this->container()->forgetInstance(ReroutClient::class);

        $client = $this->container()->make(ReroutClient::class);
        self::assertInstanceOf(ReroutClient::class, $client);

        self::assertSame('https://staging.rerout.co', $client->baseUrl());
    }

    public function testApiKeyIsReadFromConfig(): void
    {
        // A blank api_key makes the SDK throw on construction — proving the
        // config value is what the provider passes through.
        $this->config()->set('rerout.api_key', '');
        $this->container()->forgetInstance(ReroutClient::class);

        $this->expectException(\Rerout\Exceptions\ReroutException::class);
        $this->container()->make(ReroutClient::class);
    }

    public function testProviderAdvertisesItsBindings(): void
    {
        $provider = new ReroutServiceProvider($this->container());

        self::assertContains(ReroutClient::class, $provider->provides());
        self::assertContains('rerout', $provider->provides());
    }

    public function testConfigCanBePublished(): void
    {
        $this->pendingArtisan('vendor:publish', ['--tag' => 'rerout-config'])
            ->assertSuccessful();

        self::assertFileExists(config_path('rerout.php'));
        @unlink(config_path('rerout.php'));
    }
}
