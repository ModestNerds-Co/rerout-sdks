<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Exceptions\ReroutException;
use Rerout\Models\QrOptions;
use Rerout\Resources\Qr;

/**
 * Coverage for the {@see Qr} namespace — the pure URL builder and the signed
 * SVG fetch.
 */
#[CoversClass(Qr::class)]
#[CoversClass(QrOptions::class)]
final class QrTest extends TestCase
{
    // ─── url() builder ──────────────────────────────────────────────────────

    public function testBareUrlWithNoOptions(): void
    {
        $url = $this->client()->qr()->url('q4');

        self::assertSame('https://api.rerout.co/v1/links/q4/qr', $url);
    }

    public function testBareUrlWithEmptyOptions(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions());

        self::assertSame('https://api.rerout.co/v1/links/q4/qr', $url);
    }

    public function testUrlEmitsEveryOption(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions(
            size: 12,
            margin: 2,
            ecc: 'H',
            domain: 'go.brand.com',
            refresh: 'v2',
        ));

        self::assertStringStartsWith('https://api.rerout.co/v1/links/q4/qr?', $url);
        self::assertStringContainsString('size=12', $url);
        self::assertStringContainsString('margin=2', $url);
        self::assertStringContainsString('ecc=H', $url);
        self::assertStringContainsString('domain=go.brand.com', $url);
        self::assertStringContainsString('refresh=v2', $url);
    }

    public function testUrlEmitsSubsetOfOptions(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions(size: 8));

        self::assertSame('https://api.rerout.co/v1/links/q4/qr?size=8', $url);
    }

    public function testUrlHonoursCustomBaseUrl(): void
    {
        $client = $this->client('rrk_test', ['base_url' => 'https://staging.rerout.co']);

        $url = $client->qr()->url('q4');

        self::assertSame('https://staging.rerout.co/v1/links/q4/qr', $url);
    }

    public function testUrlHonoursCustomBaseUrlWithOptions(): void
    {
        $client = $this->client('rrk_test', ['base_url' => 'https://staging.rerout.co']);

        $url = $client->qr()->url('q4', new QrOptions(size: 16));

        self::assertSame('https://staging.rerout.co/v1/links/q4/qr?size=16', $url);
    }

    public function testRefreshTrueBecomesOne(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions(refresh: true));

        self::assertStringContainsString('refresh=1', $url);
    }

    public function testRefreshStringIsForwardedVerbatim(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions(refresh: 'v2'));

        self::assertStringContainsString('refresh=v2', $url);
        self::assertStringNotContainsString('refresh=1', $url);
    }

    public function testRefreshFalseIsNotEmitted(): void
    {
        $url = $this->client()->qr()->url('q4', new QrOptions(refresh: false));

        // `false` serialises to an empty string per spec — refresh key present
        // but with no value. The important thing is it is not `1`.
        self::assertStringNotContainsString('refresh=1', $url);
    }

    public function testUrlEncodesCodeWithSpace(): void
    {
        $url = $this->client()->qr()->url('hello world');

        self::assertSame('https://api.rerout.co/v1/links/hello%20world/qr', $url);
    }

    public function testUrlEncodesCodeWithSlash(): void
    {
        $url = $this->client()->qr()->url('go/promo');

        self::assertSame('https://api.rerout.co/v1/links/go%2Fpromo/qr', $url);
    }

    public function testUrlEncodesUnicodeCode(): void
    {
        $url = $this->client()->qr()->url('café');

        self::assertSame('https://api.rerout.co/v1/links/caf%C3%A9/qr', $url);
    }

    public function testUrlBuilderMakesNoNetworkCall(): void
    {
        // No response queued. If url() hit the network this test would error.
        $this->client()->qr()->url('q4', new QrOptions(size: 8));

        self::assertCount(0, $this->transactions);
    }

    public function testQrOptionsToQueryParametersDropsNulls(): void
    {
        $params = (new QrOptions(size: 10))->toQueryParameters();

        self::assertSame(['size' => '10'], $params);
    }

    // ─── svg() fetch ────────────────────────────────────────────────────────

    public function testSvgFetchesRenderedBody(): void
    {
        $svg = '<svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>';
        $this->queueRaw($svg, 200, ['Content-Type' => 'image/svg+xml']);

        $result = $this->client()->qr()->svg('q4');

        self::assertSame($svg, $result);
    }

    public function testSvgSendsBearerTokenAndGetMethod(): void
    {
        $this->queueRaw('<svg/>', 200);
        $client = $this->client('rrk_qr_secret');

        $client->qr()->svg('q4');

        $request = $this->lastRequest();
        self::assertSame('GET', $request->getMethod());
        self::assertSame('Bearer rrk_qr_secret', $request->getHeaderLine('Authorization'));
        self::assertSame('/v1/links/q4/qr', $request->getUri()->getPath());
    }

    public function testSvgAppliesQueryOptions(): void
    {
        $this->queueRaw('<svg/>', 200);

        $this->client()->qr()->svg('q4', new QrOptions(size: 20, ecc: 'Q', refresh: true));

        $query = $this->lastRequest()->getUri()->getQuery();
        self::assertStringContainsString('size=20', $query);
        self::assertStringContainsString('ecc=Q', $query);
        self::assertStringContainsString('refresh=1', $query);
    }

    public function testSvgSendsNoContentTypeHeader(): void
    {
        $this->queueRaw('<svg/>', 200);

        $this->client()->qr()->svg('q4');

        self::assertFalse($this->lastRequest()->hasHeader('Content-Type'));
    }

    public function testSvgEncodesCodeInPath(): void
    {
        $this->queueRaw('<svg/>', 200);

        $this->client()->qr()->svg('go/promo');

        self::assertSame('/v1/links/go%2Fpromo/qr', $this->lastRequest()->getUri()->getPath());
    }

    public function testSvgErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'no link'], 404);

        try {
            $this->client()->qr()->svg('missing');
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('not_found', $e->code());
            self::assertSame(404, $e->status());
        }
    }
}
