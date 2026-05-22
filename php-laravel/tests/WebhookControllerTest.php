<?php

declare(strict_types=1);

namespace Rerout\Laravel\Tests;

use Illuminate\Support\Facades\Event;
use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Laravel\Events\DomainFailed;
use Rerout\Laravel\Events\LinkClicked;
use Rerout\Laravel\Events\QrScanned;
use Rerout\Laravel\Http\WebhookController;

/**
 * Verifies the bundled webhook route: signature verification, event
 * dispatch, and HTTP status mapping.
 */
#[CoversClass(WebhookController::class)]
#[CoversClass(LinkClicked::class)]
#[CoversClass(QrScanned::class)]
#[CoversClass(DomainFailed::class)]
final class WebhookControllerTest extends TestCase
{
    private const SECRET = 'whsec_test_secret';

    /**
     * The Rerout webhook events the controller can dispatch.
     *
     * @var list<class-string>
     */
    private const REROUT_EVENTS = [LinkClicked::class, QrScanned::class, DomainFailed::class];

    /**
     * Fake only the Rerout webhook events — framework events still fire so
     * the HTTP kernel works normally.
     */
    private function fakeReroutEvents(): void
    {
        Event::fake(self::REROUT_EVENTS);
    }

    /**
     * Assert none of the Rerout webhook events were dispatched.
     */
    private function assertNoReroutEventDispatched(): void
    {
        foreach (self::REROUT_EVENTS as $event) {
            Event::assertNotDispatched($event);
        }
    }

    /**
     * Build a valid `X-Rerout-Signature` header for a body.
     */
    private function signature(string $body, ?int $timestamp = null, ?string $secret = null): string
    {
        $timestamp ??= time();
        $secret ??= self::SECRET;
        $hmac = hash_hmac('sha256', $timestamp . '.' . $body, $secret);

        return "t={$timestamp},v1={$hmac}";
    }

    /**
     * POST a JSON webhook body to the bundled route.
     *
     * @param array<string, mixed> $payload
     *
     * @return \Illuminate\Testing\TestResponse<\Symfony\Component\HttpFoundation\Response>
     */
    private function postWebhook(array $payload, ?string $signature = null): \Illuminate\Testing\TestResponse
    {
        $body = json_encode($payload, JSON_THROW_ON_ERROR);
        $signature ??= $this->signature($body);

        return $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $signature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );
    }

    // ─── Success paths ──────────────────────────────────────────────────────

    public function testValidLinkClickedWebhookReturns200(): void
    {
        Event::fake([LinkClicked::class]);

        $response = $this->postWebhook(['type' => 'link.clicked', 'data' => ['code' => 'q4']]);

        $response->assertOk();
        Event::assertDispatched(LinkClicked::class, function (LinkClicked $event): bool {
            $data = $event->payload['data'] ?? null;

            return $event->payload['type'] === 'link.clicked'
                && is_array($data)
                && ($data['code'] ?? null) === 'q4';
        });
    }

    public function testValidQrScannedWebhookDispatchesQrScanned(): void
    {
        Event::fake([QrScanned::class]);

        $response = $this->postWebhook(['type' => 'qr.scanned', 'data' => ['code' => 'q4']]);

        $response->assertOk();
        Event::assertDispatched(QrScanned::class);
    }

    public function testValidDomainFailedWebhookDispatchesDomainFailed(): void
    {
        Event::fake([DomainFailed::class]);

        $response = $this->postWebhook(['type' => 'domain.failed', 'data' => ['hostname' => 'go.brand.com']]);

        $response->assertOk();
        Event::assertDispatched(DomainFailed::class, function (DomainFailed $event): bool {
            $data = $event->payload['data'] ?? null;

            return is_array($data) && ($data['hostname'] ?? null) === 'go.brand.com';
        });
    }

    public function testUnknownEventTypeStillReturns200ButDispatchesNothing(): void
    {
        Event::fake([LinkClicked::class, QrScanned::class, DomainFailed::class]);

        $response = $this->postWebhook(['type' => 'project.updated', 'data' => []]);

        $response->assertOk();
        Event::assertNotDispatched(LinkClicked::class);
        Event::assertNotDispatched(QrScanned::class);
        Event::assertNotDispatched(DomainFailed::class);
    }

    public function testPayloadWithNoTypeReturns200WithoutDispatch(): void
    {
        Event::fake([LinkClicked::class]);

        $response = $this->postWebhook(['data' => ['code' => 'q4']]);

        $response->assertOk();
        Event::assertNotDispatched(LinkClicked::class);
    }

    public function testEventCarriesTheFullDecodedPayload(): void
    {
        /** @var array<string, mixed>|null $captured */
        $captured = null;
        Event::listen(LinkClicked::class, function (LinkClicked $event) use (&$captured): void {
            $captured = $event->payload;
        });

        $this->postWebhook([
            'type' => 'link.clicked',
            'id' => 'evt_123',
            'data' => ['code' => 'q4', 'country' => 'ZA'],
        ]);

        self::assertIsArray($captured);
        self::assertSame('evt_123', $captured['id']);

        $data = $captured['data'] ?? null;
        self::assertIsArray($data);
        self::assertSame('ZA', $data['country'] ?? null);
    }

    // ─── Signature rejection (401) ──────────────────────────────────────────

    public function testMissingSignatureHeaderReturns401(): void
    {
        $this->fakeReroutEvents();
        $body = json_encode(['type' => 'link.clicked'], JSON_THROW_ON_ERROR);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertUnauthorized();
        $this->assertNoReroutEventDispatched();
    }

    public function testWrongSecretSignatureReturns401(): void
    {
        $this->fakeReroutEvents();
        $body = json_encode(['type' => 'link.clicked'], JSON_THROW_ON_ERROR);
        $badSignature = $this->signature($body, secret: 'whsec_wrong_secret');

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $badSignature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertUnauthorized();
        $this->assertNoReroutEventDispatched();
    }

    public function testTamperedBodyReturns401(): void
    {
        $this->fakeReroutEvents();
        $original = json_encode(['type' => 'link.clicked'], JSON_THROW_ON_ERROR);
        $signature = $this->signature($original);
        $tampered = json_encode(['type' => 'link.clicked', 'extra' => 'injected'], JSON_THROW_ON_ERROR);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $signature, 'CONTENT_TYPE' => 'application/json'],
            $tampered,
        );

        $response->assertUnauthorized();
        $this->assertNoReroutEventDispatched();
    }

    public function testExpiredSignatureReturns401(): void
    {
        $this->fakeReroutEvents();
        $body = json_encode(['type' => 'link.clicked'], JSON_THROW_ON_ERROR);
        // Signed well outside the default 300s tolerance.
        $staleSignature = $this->signature($body, time() - 1000);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $staleSignature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertUnauthorized();
        $this->assertNoReroutEventDispatched();
    }

    public function testMalformedSignatureHeaderReturns401(): void
    {
        $this->fakeReroutEvents();
        $response = $this->postWebhook(['type' => 'link.clicked'], 'this is not a signature');

        $response->assertUnauthorized();
        $this->assertNoReroutEventDispatched();
    }

    // ─── Payload rejection (400) ────────────────────────────────────────────

    public function testNonJsonBodyWithValidSignatureReturns400(): void
    {
        $this->fakeReroutEvents();
        $body = 'this is not json';
        $signature = $this->signature($body);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $signature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertStatus(400);
        $this->assertNoReroutEventDispatched();
    }

    public function testJsonScalarBodyWithValidSignatureReturns400(): void
    {
        $this->fakeReroutEvents();
        // Valid JSON, but not an object/array — cannot be a webhook payload.
        $body = '"just a string"';
        $signature = $this->signature($body);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $signature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertStatus(400);
        $this->assertNoReroutEventDispatched();
    }

    // ─── Routing / config ───────────────────────────────────────────────────

    public function testWebhookRouteIsNamed(): void
    {
        $router = $this->container()->make('router');
        self::assertInstanceOf(\Illuminate\Routing\Router::class, $router);

        $route = $router->getRoutes()->getByName('rerout.webhook');

        self::assertNotNull($route);
        self::assertContains('POST', $route->methods());
    }

    public function testToleranceZeroConfigDisablesTimestampCheck(): void
    {
        $this->config()->set('rerout.webhook.tolerance', 0);
        Event::fake([LinkClicked::class]);

        $body = json_encode(['type' => 'link.clicked'], JSON_THROW_ON_ERROR);
        // Signed long ago — only accepted because tolerance is 0.
        $signature = $this->signature($body, 1);

        $response = $this->call(
            'POST',
            '/rerout/webhook',
            [],
            [],
            [],
            ['HTTP_X_REROUT_SIGNATURE' => $signature, 'CONTENT_TYPE' => 'application/json'],
            $body,
        );

        $response->assertOk();
        Event::assertDispatched(LinkClicked::class);
    }
}
