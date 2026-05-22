<?php

declare(strict_types=1);

namespace Rerout\Laravel\Tests;

use GuzzleHttp\Client;
use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\HandlerStack;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Laravel\Console\PingCommand;
use Rerout\Rerout as ReroutClient;

/**
 * Verifies `php artisan rerout:ping` against a mocked Rerout HTTP layer — no
 * real network traffic.
 */
#[CoversClass(PingCommand::class)]
final class PingCommandTest extends TestCase
{
    /**
     * Replace the bound Rerout client with one whose HTTP layer is a Guzzle
     * mock handler returning the queued responses.
     *
     * @param list<Response> $responses
     */
    private function bindMockedClient(array $responses): void
    {
        $mock = new MockHandler($responses);
        $http = new Client(['handler' => HandlerStack::create($mock)]);

        $this->container()->forgetInstance(ReroutClient::class);
        $this->container()->instance(
            ReroutClient::class,
            new ReroutClient('rrk_test_key', ['client' => $http]),
        );
    }

    public function testPingSucceedsWhenProjectResolves(): void
    {
        $this->bindMockedClient([
            new Response(200, ['Content-Type' => 'application/json'], (string) json_encode([
                'id' => 'proj_1',
                'name' => 'Acme Corp',
                'slug' => 'acme',
            ])),
        ]);

        $this->pendingArtisan('rerout:ping')
            ->expectsOutputToContain('Connected to Rerout project "Acme Corp" (acme).')
            ->assertSuccessful();
    }

    public function testPingFailsOnUnauthorized(): void
    {
        $this->bindMockedClient([
            new Response(401, ['Content-Type' => 'application/json'], (string) json_encode([
                'code' => 'unauthorized',
                'message' => 'Invalid API key.',
            ])),
        ]);

        $this->pendingArtisan('rerout:ping')
            ->expectsOutputToContain('unauthorized')
            ->assertFailed();
    }

    public function testPingFailsOnServerError(): void
    {
        $this->bindMockedClient([
            new Response(500, [], ''),
        ]);

        $this->pendingArtisan('rerout:ping')->assertFailed();
    }

    public function testCommandIsRegistered(): void
    {
        $kernel = $this->container()->make(\Illuminate\Contracts\Console\Kernel::class);
        self::assertInstanceOf(\Illuminate\Foundation\Console\Kernel::class, $kernel);

        self::assertArrayHasKey('rerout:ping', $kernel->all());
    }
}
