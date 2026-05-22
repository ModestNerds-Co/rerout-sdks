<?php

declare(strict_types=1);

namespace Rerout\Tests;

use GuzzleHttp\Exception\ConnectException;
use GuzzleHttp\Psr7\Request as Psr7Request;
use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use Rerout\Exceptions\ReroutException;
use Rerout\Rerout;
use Rerout\Resources\Links;
use Rerout\Resources\Project;
use Rerout\Resources\Qr;

/**
 * Constructor and request-transport behaviour of the {@see Rerout} client.
 */
#[CoversClass(Rerout::class)]
#[CoversClass(ReroutException::class)]
final class ReroutTest extends TestCase
{
    // ─── Constructor ────────────────────────────────────────────────────────

    public function testConstructorRequiresApiKey(): void
    {
        $this->expectException(ReroutException::class);
        $this->expectExceptionMessage('A project API key is required');

        new Rerout('');
    }

    public function testConstructorRejectsWhitespaceOnlyApiKey(): void
    {
        try {
            new Rerout('   ');
            self::fail('Expected ReroutException for blank API key.');
        } catch (ReroutException $e) {
            self::assertSame('missing_api_key', $e->code());
            self::assertSame(0, $e->status());
        }
    }

    public function testConstructorTrimsTrailingSlashesFromBaseUrl(): void
    {
        $client = $this->client('rrk_test', ['base_url' => 'https://staging.rerout.co///']);

        self::assertSame('https://staging.rerout.co', $client->baseUrl());
    }

    public function testConstructorDefaultsToProductionBaseUrl(): void
    {
        $client = $this->client();

        self::assertSame('https://api.rerout.co', $client->baseUrl());
        self::assertSame('https://api.rerout.co', Rerout::DEFAULT_BASE_URL);
    }

    public function testConstructorExposesAllThreeNamespaces(): void
    {
        $client = $this->client();

        self::assertInstanceOf(Links::class, $client->links());
        self::assertInstanceOf(Project::class, $client->project());
        self::assertInstanceOf(Qr::class, $client->qr());
    }

    public function testNamespacesAreStableSingletons(): void
    {
        $client = $this->client();

        self::assertSame($client->links(), $client->links());
        self::assertSame($client->project(), $client->project());
        self::assertSame($client->qr(), $client->qr());
    }

    // ─── Request transport ──────────────────────────────────────────────────

    public function testSendsBearerAuthorizationHeaderOnEveryCall(): void
    {
        $this->queueJson(['id' => 'p1', 'name' => 'Acme', 'slug' => 'acme']);
        $client = $this->client('rrk_secret_value');

        $client->project()->me();

        self::assertSame('Bearer rrk_secret_value', $this->lastRequest()->getHeaderLine('Authorization'));
    }

    public function testSendsAcceptJsonHeader(): void
    {
        $this->queueJson(['id' => 'p1', 'name' => 'Acme', 'slug' => 'acme']);

        $this->client()->project()->me();

        self::assertStringContainsString('application/json', $this->lastRequest()->getHeaderLine('Accept'));
    }

    public function testNoContentTypeHeaderWhenNoBodyIsSent(): void
    {
        $this->queueJson(['id' => 'p1', 'name' => 'Acme', 'slug' => 'acme']);

        $this->client()->project()->me();

        self::assertFalse($this->lastRequest()->hasHeader('Content-Type'));
    }

    public function testContentTypeJsonHeaderWhenSendingABody(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->create(new \Rerout\Models\CreateLinkInput(targetUrl: 'https://example.com'));

        self::assertSame('application/json', $this->lastRequest()->getHeaderLine('Content-Type'));
    }

    public function testSerializesJsonBody(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->create(new \Rerout\Models\CreateLinkInput(
            targetUrl: 'https://example.com/q4',
            code: 'q4',
        ));

        $body = (string) $this->lastRequest()->getBody();
        /** @var array<string, mixed> $decoded */
        $decoded = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
        self::assertSame('https://example.com/q4', $decoded['target_url']);
        self::assertSame('q4', $decoded['code']);
    }

    public function testDoesNotEscapeSlashesInJsonBody(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->create(new \Rerout\Models\CreateLinkInput(
            targetUrl: 'https://example.com/path/to/page',
        ));

        self::assertStringContainsString('https://example.com/path/to/page', (string) $this->lastRequest()->getBody());
    }

    public function testSendsCursorAndLimitQueryParams(): void
    {
        $this->queueJson(['links' => [], 'next_cursor' => null]);

        $this->client()->links()->list(cursor: 42, limit: 10);

        $query = $this->lastRequest()->getUri()->getQuery();
        self::assertStringContainsString('cursor=42', $query);
        self::assertStringContainsString('limit=10', $query);
    }

    public function testOmitsNullQueryParams(): void
    {
        $this->queueJson(['links' => [], 'next_cursor' => null]);

        $this->client()->links()->list();

        self::assertSame('', $this->lastRequest()->getUri()->getQuery());
    }

    public function testSendsDaysQueryParam(): void
    {
        $this->queueJson($this->projectStatsPayload());

        $this->client()->project()->stats(7);

        self::assertStringContainsString('days=7', $this->lastRequest()->getUri()->getQuery());
    }

    public function testIncludesDefaultHeadersOnEveryRequest(): void
    {
        $this->queueJson(['id' => 'p1', 'name' => 'Acme', 'slug' => 'acme']);
        $client = $this->client('rrk_test', ['default_headers' => ['User-Agent' => 'my-app/2.0']]);

        $client->project()->me();

        self::assertSame('my-app/2.0', $this->lastRequest()->getHeaderLine('User-Agent'));
    }

    public function testTargetsTheConfiguredBaseUrl(): void
    {
        $this->queueJson(['id' => 'p1', 'name' => 'Acme', 'slug' => 'acme']);
        $client = $this->client('rrk_test', ['base_url' => 'https://staging.rerout.co']);

        $client->project()->me();

        self::assertSame('https://staging.rerout.co/v1/projects/me', (string) $this->lastRequest()->getUri());
    }

    // ─── Error parsing ──────────────────────────────────────────────────────

    public function testPreservesServerErrorCodeAndMessage(): void
    {
        $this->queueJson([
            'code' => 'bad_target_url',
            'message' => 'target_url must use https.',
        ], 400);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('bad_target_url', $e->code());
            self::assertSame('target_url must use https.', $e->getMessage());
            self::assertSame(400, $e->status());
        }
    }

    public function testCapturesServerProvidedTimestampAndDetails(): void
    {
        $this->queueJson([
            'code' => 'rate_limited',
            'message' => 'Slow down.',
            'timestamp' => '2026-05-20T10:00:00Z',
            'path' => '/v1/links/abc',
        ], 429);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('2026-05-20T10:00:00Z', $e->timestamp);
            self::assertIsArray($e->details);
            self::assertSame('rate_limited', $e->details['code']);
        }
    }

    /**
     * @return array<string, array{0: int, 1: string}>
     */
    public static function syntheticCodeProvider(): array
    {
        return [
            '401 → unauthorized' => [401, 'unauthorized'],
            '403 → forbidden' => [403, 'forbidden'],
            '404 → not_found' => [404, 'not_found'],
            '429 → rate_limited' => [429, 'rate_limited'],
            '500 → server_error' => [500, 'server_error'],
            '503 → server_error' => [503, 'server_error'],
            '400 → client_error' => [400, 'client_error'],
            '418 → client_error' => [418, 'client_error'],
        ];
    }

    #[DataProvider('syntheticCodeProvider')]
    public function testSyntheticCodeForStatusWithNoBody(int $status, string $expectedCode): void
    {
        $this->queueRaw('', $status);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame($expectedCode, $e->code());
            self::assertSame($status, $e->status());
            self::assertStringContainsString('no body', $e->getMessage());
        }
    }

    public function testSyntheticCodeForNonJsonErrorBody(): void
    {
        $this->queueRaw('<html>500</html>', 500);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('server_error', $e->code());
            self::assertSame(500, $e->status());
        }
    }

    public function testNetworkFailureMapsToNetworkErrorCode(): void
    {
        $this->mock->append(new ConnectException(
            'Could not resolve host',
            new Psr7Request('GET', 'https://api.rerout.co/v1/links/abc'),
        ));

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('network_error', $e->code());
            self::assertSame(0, $e->status());
        }
    }

    public function testTimeoutMapsToTimeoutCode(): void
    {
        $this->mock->append(new ConnectException(
            'cURL error 28: Operation timed out after 30000 milliseconds',
            new Psr7Request('GET', 'https://api.rerout.co/v1/links/abc'),
        ));

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('timeout', $e->code());
            self::assertSame(0, $e->status());
        }
    }

    public function testUnexpectedResponseOnTwoxxNonJsonBody(): void
    {
        $this->queueRaw('not json at all', 200);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('unexpected_response', $e->code());
            self::assertSame(200, $e->status());
        }
    }

    public function testUnexpectedResponseOnTwoxxJsonArrayBody(): void
    {
        // A bare JSON array decodes fine but isn't the object shape the
        // client expects for a Link payload.
        $this->queueRaw('"a bare string"', 200);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('unexpected_response', $e->code());
        }
    }

    // ─── Exception convenience flags ────────────────────────────────────────

    public function testIsRateLimitedFlag(): void
    {
        $this->queueRaw('', 429);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertTrue($e->isRateLimited());
            self::assertFalse($e->isServerError());
        }
    }

    public function testIsServerErrorFlag(): void
    {
        $this->queueRaw('', 502);

        try {
            $this->client()->links()->get('abc');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertTrue($e->isServerError());
            self::assertFalse($e->isRateLimited());
        }
    }

    public function testExceptionToStringContainsCodeAndStatus(): void
    {
        $e = new ReroutException(
            errorCode: 'not_found',
            message: 'Missing.',
            status: 404,
            path: '/v1/links/abc',
        );

        $string = (string) $e;
        self::assertStringContainsString('not_found', $string);
        self::assertStringContainsString('404', $string);
        self::assertStringContainsString('/v1/links/abc', $string);
    }

    // ─── Fixtures ───────────────────────────────────────────────────────────

    /**
     * @return array<string, mixed>
     */
    private function linkPayload(): array
    {
        return [
            'code' => 'q4',
            'short_url' => 'https://rerout.co/q4',
            'domain_hostname' => null,
            'target_url' => 'https://example.com',
            'project_id' => 'proj_1',
            'expires_at' => null,
            'is_active' => true,
            'seo_title' => null,
            'seo_description' => null,
            'seo_image_url' => null,
            'seo_canonical_url' => null,
            'seo_noindex' => false,
            'seo_updated_at' => null,
            'created_at' => 1_700_000_000,
            'updated_at' => 1_700_000_000,
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function projectStatsPayload(): array
    {
        return [
            'days' => 7,
            'total_clicks' => 0,
            'qr_scans' => 0,
            'daily' => [],
            'countries' => [],
            'referrers' => [],
            'devices' => [],
            'browsers' => [],
            'top_codes' => [],
        ];
    }
}
