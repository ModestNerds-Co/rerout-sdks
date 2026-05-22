<?php

declare(strict_types=1);

namespace Rerout\Tests;

use GuzzleHttp\Client;
use GuzzleHttp\Handler\MockHandler;
use GuzzleHttp\HandlerStack;
use GuzzleHttp\Middleware;
use GuzzleHttp\Psr7\Request;
use GuzzleHttp\Psr7\Response;
use PHPUnit\Framework\TestCase as BaseTestCase;
use Rerout\Rerout;

/**
 * Shared base for the Rerout SDK test suite.
 *
 * Builds a {@see Rerout} client backed by a Guzzle {@see MockHandler} so no
 * test ever touches the real network. Every request the client sends is
 * captured in {@see TestCase::$transactions} for assertion.
 */
abstract class TestCase extends BaseTestCase
{
    /**
     * Captured request/response history from the mock handler.
     *
     * @var list<array{request: Request, response: Response|null, error: mixed, options: array<string, mixed>}>
     */
    protected array $transactions = [];

    protected MockHandler $mock;

    protected function setUp(): void
    {
        parent::setUp();
        $this->transactions = [];
        $this->mock = new MockHandler();
    }

    /**
     * Build a Rerout client whose HTTP layer is the mock handler. Queue
     * responses with {@see TestCase::queue()} before calling SDK methods.
     *
     * @param array{base_url?: string, timeout?: int, default_headers?: array<string, string>} $options
     */
    protected function client(string $apiKey = 'rrk_test_key', array $options = []): Rerout
    {
        $stack = HandlerStack::create($this->mock);
        $stack->push(Middleware::history($this->transactions));
        $http = new Client(['handler' => $stack]);

        /** @var array{base_url?: string, timeout?: int, client?: \GuzzleHttp\ClientInterface, default_headers?: array<string, string>} $merged */
        $merged = array_merge($options, ['client' => $http]);

        return new Rerout($apiKey, $merged);
    }

    /**
     * Queue a JSON response on the mock handler.
     *
     * @param array<string, mixed>|list<mixed> $body
     * @param array<string, string>            $headers
     */
    protected function queueJson(array $body, int $status = 200, array $headers = []): void
    {
        $encoded = json_encode($body, JSON_THROW_ON_ERROR);
        $this->mock->append(new Response(
            $status,
            array_merge(['Content-Type' => 'application/json'], $headers),
            $encoded,
        ));
    }

    /**
     * Queue a raw (already-encoded) response body on the mock handler.
     *
     * @param array<string, string> $headers
     */
    protected function queueRaw(string $body, int $status = 200, array $headers = []): void
    {
        $this->mock->append(new Response($status, $headers, $body));
    }

    /**
     * The most recent request the client actually sent.
     */
    protected function lastRequest(): Request
    {
        $count = count($this->transactions);
        if ($count === 0) {
            self::fail('No request was sent to the mock handler.');
        }

        $request = $this->transactions[$count - 1]['request'];
        self::assertInstanceOf(Request::class, $request);

        return $request;
    }
}
