<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use Rerout\Exceptions\ReroutException;
use Rerout\Models\CreateLinkInput;
use Rerout\Models\Link;
use Rerout\Models\LinkStats;
use Rerout\Models\ListLinksResult;
use Rerout\Models\ProjectStats;
use Rerout\Models\UpdateLinkInput;
use Rerout\Resources\Links;
use Rerout\Resources\Project;

/**
 * Coverage for every {@see Links} and {@see Project} method, plus URL-encoding
 * edge cases for link codes.
 */
#[CoversClass(Links::class)]
#[CoversClass(Project::class)]
#[CoversClass(Link::class)]
#[CoversClass(LinkStats::class)]
#[CoversClass(ListLinksResult::class)]
#[CoversClass(ProjectStats::class)]
#[CoversClass(CreateLinkInput::class)]
#[CoversClass(UpdateLinkInput::class)]
final class LinksTest extends TestCase
{
    // ─── create ─────────────────────────────────────────────────────────────

    public function testCreateSendsPostToLinksEndpoint(): void
    {
        $this->queueJson($this->linkPayload());

        $link = $this->client()->links()->create(new CreateLinkInput(targetUrl: 'https://example.com'));

        $request = $this->lastRequest();
        self::assertSame('POST', $request->getMethod());
        self::assertSame('/v1/links', $request->getUri()->getPath());
        self::assertInstanceOf(Link::class, $link);
        self::assertSame('q4', $link->code);
        self::assertSame('https://rerout.co/q4', $link->shortUrl);
        self::assertTrue($link->isActive);
    }

    public function testCreateSerializesAllProvidedFields(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->create(new CreateLinkInput(
            targetUrl: 'https://example.com/sale',
            domainHostname: 'go.brand.com',
            code: 'q4',
            expiresAt: 1_800_000_000,
            seoTitle: 'Big Sale',
            seoDescription: 'Up to 50% off',
            seoImageUrl: 'https://cdn.example.com/og.png',
            seoCanonicalUrl: 'https://example.com/canonical',
            seoNoindex: true,
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame('https://example.com/sale', $body['target_url']);
        self::assertSame('go.brand.com', $body['domain_hostname']);
        self::assertSame('q4', $body['code']);
        self::assertSame(1_800_000_000, $body['expires_at']);
        self::assertSame('Big Sale', $body['seo_title']);
        self::assertSame('Up to 50% off', $body['seo_description']);
        self::assertSame('https://cdn.example.com/og.png', $body['seo_image_url']);
        self::assertSame('https://example.com/canonical', $body['seo_canonical_url']);
        self::assertTrue($body['seo_noindex']);
    }

    public function testCreateOmitsUnsetOptionalFields(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->create(new CreateLinkInput(targetUrl: 'https://example.com'));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['target_url' => 'https://example.com'], $body);
    }

    public function testCreateErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'bad_target_url', 'message' => 'must be https'], 400);

        $this->expectException(ReroutException::class);
        $this->client()->links()->create(new CreateLinkInput(targetUrl: 'http://insecure'));
    }

    // ─── list ───────────────────────────────────────────────────────────────

    public function testListReturnsParsedResult(): void
    {
        $this->queueJson([
            'links' => [$this->linkPayload(), $this->linkPayload()],
            'next_cursor' => 99,
        ]);

        $result = $this->client()->links()->list();

        self::assertInstanceOf(ListLinksResult::class, $result);
        self::assertCount(2, $result->links);
        self::assertContainsOnlyInstancesOf(Link::class, $result->links);
        self::assertSame(99, $result->nextCursor);
    }

    public function testListHandlesNullCursor(): void
    {
        $this->queueJson(['links' => [], 'next_cursor' => null]);

        $result = $this->client()->links()->list();

        self::assertSame([], $result->links);
        self::assertNull($result->nextCursor);
    }

    public function testListUsesGetMethod(): void
    {
        $this->queueJson(['links' => [], 'next_cursor' => null]);

        $this->client()->links()->list(cursor: 5, limit: 25);

        self::assertSame('GET', $this->lastRequest()->getMethod());
    }

    public function testListErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'unauthorized', 'message' => 'bad key'], 401);

        $this->expectException(ReroutException::class);
        $this->client()->links()->list();
    }

    // ─── get ────────────────────────────────────────────────────────────────

    public function testGetReturnsLink(): void
    {
        $this->queueJson($this->linkPayload());

        $link = $this->client()->links()->get('q4');

        $request = $this->lastRequest();
        self::assertSame('GET', $request->getMethod());
        self::assertSame('/v1/links/q4', $request->getUri()->getPath());
        self::assertSame('q4', $link->code);
    }

    public function testGetErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'no link'], 404);

        try {
            $this->client()->links()->get('missing');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('not_found', $e->code());
        }
    }

    // ─── update ─────────────────────────────────────────────────────────────

    public function testUpdateSendsPatch(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(targetUrl: 'https://example.com/new'));

        $request = $this->lastRequest();
        self::assertSame('PATCH', $request->getMethod());
        self::assertSame('/v1/links/q4', $request->getUri()->getPath());
    }

    public function testUpdateOnlySendsSetFields(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(isActive: false));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['is_active' => false], $body);
    }

    public function testUpdateClearSentinelSerializesAsExplicitNull(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(
            expiresAt: UpdateLinkInput::CLEAR,
            seoTitle: UpdateLinkInput::CLEAR,
        ));

        $raw = (string) $this->lastRequest()->getBody();
        /** @var array<string, mixed> $body */
        $body = json_decode($raw, true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('expires_at', $body);
        self::assertNull($body['expires_at']);
        self::assertArrayHasKey('seo_title', $body);
        self::assertNull($body['seo_title']);
        self::assertStringContainsString('"expires_at":null', $raw);
    }

    public function testUpdateDistinguishesLeaveAloneFromClear(): void
    {
        $this->queueJson($this->linkPayload());

        // expiresAt cleared, seoTitle left alone (default UNSET).
        $this->client()->links()->update('q4', new UpdateLinkInput(expiresAt: UpdateLinkInput::CLEAR));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('expires_at', $body);
        self::assertArrayNotHasKey('seo_title', $body);
    }

    public function testUpdateWithConcreteValuesSerializesThem(): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(
            targetUrl: 'https://example.com/v2',
            expiresAt: 1_900_000_000,
            isActive: true,
            seoNoindex: false,
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame('https://example.com/v2', $body['target_url']);
        self::assertSame(1_900_000_000, $body['expires_at']);
        self::assertTrue($body['is_active']);
        self::assertFalse($body['seo_noindex']);
    }

    public function testUpdateWithEmptyInputThrowsBeforeHittingTheApi(): void
    {
        // No response queued — if the client hit the network this would fail
        // differently. The empty-input guard must short-circuit client-side.
        try {
            $this->client()->links()->update('q4', new UpdateLinkInput());
            self::fail('Expected ReroutException for empty UpdateLinkInput.');
        } catch (ReroutException $e) {
            self::assertSame('bad_request', $e->code());
            self::assertSame(0, $e->status());
        }

        self::assertCount(0, $this->transactions, 'Empty update must not send a request.');
    }

    public function testUpdateInputToArrayThrowsWhenEmpty(): void
    {
        $this->expectException(ReroutException::class);
        (new UpdateLinkInput())->toArray();
    }

    public function testUpdateInputIsEmptyReportsCorrectly(): void
    {
        self::assertTrue((new UpdateLinkInput())->isEmpty());
        self::assertFalse((new UpdateLinkInput(isActive: true))->isEmpty());
    }

    public function testUpdateErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->links()->update('q4', new UpdateLinkInput(isActive: false));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    public function testDeleteSendsDeleteAndReturnsTrue(): void
    {
        $this->queueJson(['deleted' => true]);

        $deleted = $this->client()->links()->delete('q4');

        $request = $this->lastRequest();
        self::assertSame('DELETE', $request->getMethod());
        self::assertSame('/v1/links/q4', $request->getUri()->getPath());
        self::assertTrue($deleted);
    }

    public function testDeleteReturnsFalseWhenServerReportsNotDeleted(): void
    {
        $this->queueJson(['deleted' => false]);

        self::assertFalse($this->client()->links()->delete('q4'));
    }

    public function testDeleteReturnsTrueOnEmptyBody(): void
    {
        $this->queueRaw('', 204);

        self::assertTrue($this->client()->links()->delete('q4'));
    }

    public function testDeleteErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->links()->delete('q4');
    }

    // ─── stats ──────────────────────────────────────────────────────────────

    public function testStatsReturnsParsedLinkStats(): void
    {
        $this->queueJson([
            'code' => 'q4',
            'days' => 30,
            'total_clicks' => 1234,
            'qr_scans' => 56,
            'countries' => [['value' => 'ZA', 'clicks' => 800], ['value' => 'US', 'clicks' => 434]],
            'referrers' => [['value' => 'twitter.com', 'clicks' => 300]],
        ]);

        $stats = $this->client()->links()->stats('q4');

        self::assertInstanceOf(LinkStats::class, $stats);
        self::assertSame('q4', $stats->code);
        self::assertSame(1234, $stats->totalClicks);
        self::assertSame(56, $stats->qrScans);
        self::assertCount(2, $stats->countries);
        self::assertSame('ZA', $stats->countries[0]->value);
        self::assertSame(800, $stats->countries[0]->clicks);
        self::assertCount(1, $stats->referrers);
    }

    public function testStatsDefaultsToThirtyDays(): void
    {
        $this->queueJson($this->linkStatsPayload());

        $this->client()->links()->stats('q4');

        self::assertStringContainsString('days=30', $this->lastRequest()->getUri()->getQuery());
        self::assertSame('/v1/links/q4/stats', $this->lastRequest()->getUri()->getPath());
    }

    public function testStatsHonoursCustomWindow(): void
    {
        $this->queueJson($this->linkStatsPayload());

        $this->client()->links()->stats('q4', 90);

        self::assertStringContainsString('days=90', $this->lastRequest()->getUri()->getQuery());
    }

    public function testStatsErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->links()->stats('missing');
    }

    // ─── project ────────────────────────────────────────────────────────────

    public function testProjectStatsReturnsParsedStats(): void
    {
        $this->queueJson([
            'days' => 7,
            'total_clicks' => 500,
            'qr_scans' => 40,
            'daily' => [
                ['day' => 1_700_000_000, 'clicks' => 100, 'qr_scans' => 10],
                ['day' => 1_700_086_400, 'clicks' => 400, 'qr_scans' => 30],
            ],
            'countries' => [['value' => 'ZA', 'clicks' => 500]],
            'referrers' => [['value' => 'direct', 'clicks' => 250]],
            'devices' => [['value' => 'mobile', 'clicks' => 300]],
            'browsers' => [['value' => 'chrome', 'clicks' => 350]],
            'top_codes' => [['value' => 'q4', 'clicks' => 500]],
        ]);

        $stats = $this->client()->project()->stats(7);

        self::assertInstanceOf(ProjectStats::class, $stats);
        self::assertSame(7, $stats->days);
        self::assertSame(500, $stats->totalClicks);
        self::assertSame(40, $stats->qrScans);
        self::assertCount(2, $stats->daily);
        self::assertSame(100, $stats->daily[0]->clicks);
        self::assertSame(10, $stats->daily[0]->qrScans);
        self::assertCount(1, $stats->countries);
        self::assertCount(1, $stats->devices);
        self::assertCount(1, $stats->browsers);
        self::assertCount(1, $stats->topCodes);
    }

    public function testProjectStatsDefaultsToThirtyDays(): void
    {
        $this->queueJson($this->projectStatsPayload());

        $this->client()->project()->stats();

        self::assertStringContainsString('days=30', $this->lastRequest()->getUri()->getQuery());
        self::assertSame('/v1/projects/me/stats', $this->lastRequest()->getUri()->getPath());
    }

    public function testProjectMeReturnsRawArray(): void
    {
        $this->queueJson(['id' => 'proj_1', 'name' => 'Acme Corp', 'slug' => 'acme']);

        $me = $this->client()->project()->me();

        self::assertSame('/v1/projects/me', $this->lastRequest()->getUri()->getPath());
        self::assertSame('proj_1', $me['id']);
        self::assertSame('Acme Corp', $me['name']);
        self::assertSame('acme', $me['slug']);
    }

    public function testProjectStatsErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'server_error', 'message' => 'boom'], 500);

        $this->expectException(ReroutException::class);
        $this->client()->project()->stats();
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    /**
     * @return array<string, array{0: string, 1: string}>
     */
    public static function codeEncodingProvider(): array
    {
        return [
            'space' => ['hello world', '/v1/links/hello%20world'],
            'plus' => ['a+b', '/v1/links/a%2Bb'],
            'unicode' => ['café', '/v1/links/caf%C3%A9'],
            'slash in code' => ['go/promo', '/v1/links/go%2Fpromo'],
            'ampersand' => ['a&b', '/v1/links/a%26b'],
            'question mark' => ['who?', '/v1/links/who%3F'],
            'hash' => ['a#b', '/v1/links/a%23b'],
        ];
    }

    #[DataProvider('codeEncodingProvider')]
    public function testGetEncodesCodeInPath(string $code, string $expectedPath): void
    {
        $this->queueJson($this->linkPayload());

        $this->client()->links()->get($code);

        self::assertSame($expectedPath, $this->lastRequest()->getUri()->getPath());
    }

    #[DataProvider('codeEncodingProvider')]
    public function testStatsEncodesCodeInPath(string $code, string $expectedPath): void
    {
        $this->queueJson($this->linkStatsPayload());

        $this->client()->links()->stats($code);

        self::assertSame($expectedPath . '/stats', $this->lastRequest()->getUri()->getPath());
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
    private function linkStatsPayload(): array
    {
        return [
            'code' => 'q4',
            'days' => 30,
            'total_clicks' => 0,
            'qr_scans' => 0,
            'countries' => [],
            'referrers' => [],
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function projectStatsPayload(): array
    {
        return [
            'days' => 30,
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
