<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use Rerout\Exceptions\ReroutException;
use Rerout\Models\CreatedWebhook;
use Rerout\Models\CreateWebhookInput;
use Rerout\Models\ListWebhooksResult;
use Rerout\Models\Webhook;
use Rerout\Resources\Webhooks;

/**
 * Coverage for the {@see Webhooks} endpoint-management namespace
 * (`create` / `list` / `delete`) against a mocked HTTP layer.
 */
#[CoversClass(Webhooks::class)]
#[CoversClass(Webhook::class)]
#[CoversClass(CreateWebhookInput::class)]
#[CoversClass(CreatedWebhook::class)]
#[CoversClass(ListWebhooksResult::class)]
final class WebhooksManagementTest extends TestCase
{
    // ─── create ─────────────────────────────────────────────────────────────

    public function testCreatePostsToWebhooksEndpointAndReturnsSigningSecret(): void
    {
        $this->queueJson([
            'endpoint' => $this->webhookPayload(),
            'signing_secret' => 'whsec_supersecret',
        ], 201);

        $result = $this->client()->webhooks()->create(new CreateWebhookInput(
            name: 'Order events',
            url: 'https://example.com/hooks/rerout',
            events: ['link.created', 'link.clicked'],
        ));

        $request = $this->lastRequest();
        self::assertSame('POST', $request->getMethod());
        self::assertSame('/v1/projects/me/webhooks', $request->getUri()->getPath());

        self::assertInstanceOf(CreatedWebhook::class, $result);
        self::assertSame('whsec_supersecret', $result->signingSecret);
        self::assertInstanceOf(Webhook::class, $result->endpoint);
        self::assertSame('wh_abc123', $result->endpoint->id);
        self::assertSame('Order events', $result->endpoint->name);
        self::assertSame(['link.created', 'link.clicked'], $result->endpoint->events);
        self::assertTrue($result->endpoint->isActive);
    }

    public function testCreateSendsRequiredFieldsOnly(): void
    {
        $this->queueJson([
            'endpoint' => $this->webhookPayload(),
            'signing_secret' => 'whsec_x',
        ], 201);

        $this->client()->webhooks()->create(new CreateWebhookInput(
            name: 'Order events',
            url: 'https://example.com/hooks/rerout',
            events: ['link.created', 'link.clicked'],
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame([
            'name' => 'Order events',
            'url' => 'https://example.com/hooks/rerout',
            'events' => ['link.created', 'link.clicked'],
        ], $body);
        self::assertArrayNotHasKey('is_active', $body);
        self::assertArrayNotHasKey('payload_format', $body);
    }

    public function testCreateForwardsIsActiveAndPayloadFormatWhenProvided(): void
    {
        $this->queueJson([
            'endpoint' => array_merge($this->webhookPayload(), [
                'is_active' => false,
                'payload_format' => 'slack',
            ]),
            'signing_secret' => 'whsec_x',
        ], 201);

        $result = $this->client()->webhooks()->create(new CreateWebhookInput(
            name: 'Slack',
            url: 'https://hooks.slack.com/services/T/B/x',
            events: ['link.created'],
            isActive: false,
            payloadFormat: 'slack',
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertFalse($body['is_active']);
        self::assertSame('slack', $body['payload_format']);

        self::assertFalse($result->endpoint->isActive);
        self::assertSame('slack', $result->endpoint->payloadFormat);
    }

    public function testCreateErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'bad_request', 'message' => 'url must be https'], 400);

        $this->expectException(ReroutException::class);
        $this->client()->webhooks()->create(new CreateWebhookInput(
            name: 'Bad',
            url: 'http://insecure',
            events: ['link.created'],
        ));
    }

    // ─── list ───────────────────────────────────────────────────────────────

    public function testListReturnsEndpointsAndEventTypes(): void
    {
        $this->queueJson([
            'endpoints' => [$this->webhookPayload(), $this->webhookPayload()],
            'event_types' => ['link.created', 'link.clicked', 'domain.verified'],
        ]);

        $result = $this->client()->webhooks()->list();

        $request = $this->lastRequest();
        self::assertSame('GET', $request->getMethod());
        self::assertSame('/v1/projects/me/webhooks', $request->getUri()->getPath());

        self::assertInstanceOf(ListWebhooksResult::class, $result);
        self::assertCount(2, $result->endpoints);
        self::assertContainsOnlyInstancesOf(Webhook::class, $result->endpoints);
        self::assertSame('https://example.com/hooks/rerout', $result->endpoints[0]->url);
        self::assertContains('domain.verified', $result->eventTypes);
    }

    public function testListHandlesEmptyResult(): void
    {
        $this->queueJson(['endpoints' => [], 'event_types' => []]);

        $result = $this->client()->webhooks()->list();

        self::assertSame([], $result->endpoints);
        self::assertSame([], $result->eventTypes);
    }

    public function testListErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'unauthorized', 'message' => 'bad key'], 401);

        $this->expectException(ReroutException::class);
        $this->client()->webhooks()->list();
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    public function testDeleteSendsDeleteAndReturnsTrue(): void
    {
        $this->queueJson(['deleted' => true]);

        $deleted = $this->client()->webhooks()->delete('wh_abc123');

        $request = $this->lastRequest();
        self::assertSame('DELETE', $request->getMethod());
        self::assertSame('/v1/projects/me/webhooks/wh_abc123', $request->getUri()->getPath());
        self::assertTrue($deleted);
    }

    public function testDeleteReturnsFalseWhenServerReportsNotDeleted(): void
    {
        $this->queueJson(['deleted' => false]);

        self::assertFalse($this->client()->webhooks()->delete('wh_abc123'));
    }

    public function testDeleteReturnsTrueOnEmptyBody(): void
    {
        $this->queueRaw('', 204);

        self::assertTrue($this->client()->webhooks()->delete('wh_abc123'));
    }

    public function testDeleteErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->webhooks()->delete('wh_missing');
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    /**
     * @return array<string, array{0: string, 1: string}>
     */
    public static function endpointIdEncodingProvider(): array
    {
        return [
            'plain' => ['wh_abc123', '/v1/projects/me/webhooks/wh_abc123'],
            'space' => ['wh abc', '/v1/projects/me/webhooks/wh%20abc'],
            'slash' => ['wh/abc', '/v1/projects/me/webhooks/wh%2Fabc'],
            'ampersand' => ['wh&abc', '/v1/projects/me/webhooks/wh%26abc'],
            'question mark' => ['wh?abc', '/v1/projects/me/webhooks/wh%3Fabc'],
            'hash' => ['wh#abc', '/v1/projects/me/webhooks/wh%23abc'],
        ];
    }

    #[DataProvider('endpointIdEncodingProvider')]
    public function testDeleteEncodesEndpointIdInPath(string $endpointId, string $expectedPath): void
    {
        $this->queueJson(['deleted' => true]);

        $this->client()->webhooks()->delete($endpointId);

        self::assertSame($expectedPath, $this->lastRequest()->getUri()->getPath());
    }

    // ─── Fixtures ───────────────────────────────────────────────────────────

    /**
     * @return array<string, mixed>
     */
    private function webhookPayload(): array
    {
        return [
            'id' => 'wh_abc123',
            'project_id' => 'prj_test',
            'name' => 'Order events',
            'url' => 'https://example.com/hooks/rerout',
            'events' => ['link.created', 'link.clicked'],
            'is_active' => true,
            'payload_format' => 'json',
            'created_at' => 1_700_000_000,
            'updated_at' => 1_700_000_000,
            'last_delivery_at' => null,
            'last_success_at' => null,
            'last_failure_at' => null,
        ];
    }
}
