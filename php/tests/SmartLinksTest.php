<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Models\AbVariant;
use Rerout\Models\AbVariantInput;
use Rerout\Models\BatchCreateLinksResult;
use Rerout\Models\BatchLinkInput;
use Rerout\Models\BatchLinkResult;
use Rerout\Models\CreateLinkInput;
use Rerout\Models\Link;
use Rerout\Models\RoutingRule;
use Rerout\Models\UpdateLinkInput;
use Rerout\Resources\Links;

/**
 * Coverage for the Smart Links surface: the password / max-click / conversion /
 * routing-rule / A/B-variant fields on {@see Link}, {@see CreateLinkInput}, and
 * {@see UpdateLinkInput}, plus the {@see Links::createBatch} batch endpoint.
 */
#[CoversClass(Links::class)]
#[CoversClass(Link::class)]
#[CoversClass(CreateLinkInput::class)]
#[CoversClass(UpdateLinkInput::class)]
#[CoversClass(RoutingRule::class)]
#[CoversClass(AbVariant::class)]
#[CoversClass(AbVariantInput::class)]
#[CoversClass(BatchLinkInput::class)]
#[CoversClass(BatchLinkResult::class)]
#[CoversClass(BatchCreateLinksResult::class)]
final class SmartLinksTest extends TestCase
{
    // ─── Link response parsing ──────────────────────────────────────────────

    public function testLinkParsesSmartLinkFields(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $link = $this->client()->links()->get('q4');

        self::assertTrue($link->passwordProtected);
        self::assertSame(1000, $link->maxClicks);
        self::assertSame(42, $link->clickCount);
        self::assertTrue($link->trackConversions);

        self::assertCount(1, $link->routingRules);
        self::assertContainsOnlyInstancesOf(RoutingRule::class, $link->routingRules);
        self::assertSame('country', $link->routingRules[0]->conditionType);
        self::assertSame('is', $link->routingRules[0]->conditionOp);
        self::assertSame('ZA', $link->routingRules[0]->conditionValue);
        self::assertSame('https://example.com/za', $link->routingRules[0]->targetUrl);

        self::assertCount(2, $link->abVariants);
        self::assertContainsOnlyInstancesOf(AbVariant::class, $link->abVariants);
        self::assertSame(1, $link->abVariants[0]->id);
        self::assertSame('https://example.com/a', $link->abVariants[0]->targetUrl);
        self::assertSame(70, $link->abVariants[0]->weight);
    }

    public function testLinkDefaultsSmartLinkFieldsWhenAbsent(): void
    {
        // The classic (pre-Smart-Links) payload has none of the new fields.
        $this->queueJson($this->classicLinkPayload());

        $link = $this->client()->links()->get('q4');

        self::assertFalse($link->passwordProtected);
        self::assertNull($link->maxClicks);
        self::assertSame(0, $link->clickCount);
        self::assertFalse($link->trackConversions);
        self::assertSame([], $link->routingRules);
        self::assertSame([], $link->abVariants);
    }

    public function testLinkParsesNullMaxClicks(): void
    {
        $payload = $this->smartLinkPayload();
        $payload['max_clicks'] = null;
        $this->queueJson($payload);

        $link = $this->client()->links()->get('q4');

        self::assertNull($link->maxClicks);
    }

    // ─── CreateLinkInput serialization ──────────────────────────────────────

    public function testCreateOmitsSmartLinkFieldsWhenUnset(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $this->client()->links()->create(new CreateLinkInput(targetUrl: 'https://example.com'));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayNotHasKey('password', $body);
        self::assertArrayNotHasKey('max_clicks', $body);
        self::assertArrayNotHasKey('track_conversions', $body);
        self::assertArrayNotHasKey('routing_rules', $body);
        self::assertArrayNotHasKey('ab_variants', $body);
    }

    public function testCreateSerializesSmartLinkFields(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $this->client()->links()->create(new CreateLinkInput(
            targetUrl: 'https://example.com',
            password: 's3cret',
            maxClicks: 500,
            trackConversions: true,
            routingRules: [
                new RoutingRule('device', 'in', 'mobile,tablet', 'https://example.com/m'),
            ],
            abVariants: [
                new AbVariantInput('https://example.com/a', 60),
                new AbVariantInput('https://example.com/b'),
            ],
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame('s3cret', $body['password']);
        self::assertSame(500, $body['max_clicks']);
        self::assertTrue($body['track_conversions']);

        self::assertSame([
            [
                'condition_type' => 'device',
                'condition_op' => 'in',
                'condition_value' => 'mobile,tablet',
                'target_url' => 'https://example.com/m',
            ],
        ], $body['routing_rules']);

        self::assertSame([
            ['target_url' => 'https://example.com/a', 'weight' => 60],
            ['target_url' => 'https://example.com/b'],
        ], $body['ab_variants']);
    }

    // ─── UpdateLinkInput serialization ──────────────────────────────────────

    public function testUpdateSerializesSmartLinkFields(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(
            password: 'newpass',
            maxClicks: 250,
            trackConversions: true,
            routingRules: [new RoutingRule('country', 'is_not', 'US', 'https://example.com/intl')],
            abVariants: [new AbVariantInput('https://example.com/a', 50)],
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame('newpass', $body['password']);
        self::assertSame(250, $body['max_clicks']);
        self::assertTrue($body['track_conversions']);

        /** @var array<int, array<string, mixed>> $rules */
        $rules = $body['routing_rules'];
        self::assertSame('country', $rules[0]['condition_type']);

        /** @var array<int, array<string, mixed>> $variants */
        $variants = $body['ab_variants'];
        self::assertSame('https://example.com/a', $variants[0]['target_url']);
    }

    public function testUpdateClearsPasswordAndMaxClicks(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(
            password: UpdateLinkInput::CLEAR,
            maxClicks: UpdateLinkInput::CLEAR,
        ));

        $raw = (string) $this->lastRequest()->getBody();
        /** @var array<string, mixed> $body */
        $body = json_decode($raw, true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('password', $body);
        self::assertNull($body['password']);
        self::assertArrayHasKey('max_clicks', $body);
        self::assertNull($body['max_clicks']);
        self::assertStringContainsString('"max_clicks":null', $raw);
    }

    public function testUpdateAllowsEmptyRoutingRulesAsFullReplace(): void
    {
        $this->queueJson($this->smartLinkPayload());

        // An empty array is a meaningful "replace with nothing", distinct from
        // leaving the field alone (UNSET).
        $this->client()->links()->update('q4', new UpdateLinkInput(routingRules: []));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('routing_rules', $body);
        self::assertSame([], $body['routing_rules']);
    }

    public function testUpdateLeavesSmartLinkFieldsAloneByDefault(): void
    {
        $this->queueJson($this->smartLinkPayload());

        $this->client()->links()->update('q4', new UpdateLinkInput(isActive: true));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['is_active' => true], $body);
    }

    public function testUpdateIsEmptyAccountsForSmartLinkFields(): void
    {
        self::assertFalse((new UpdateLinkInput(password: 'x'))->isEmpty());
        self::assertFalse((new UpdateLinkInput(maxClicks: 10))->isEmpty());
        self::assertFalse((new UpdateLinkInput(trackConversions: true))->isEmpty());
        self::assertFalse((new UpdateLinkInput(routingRules: []))->isEmpty());
        self::assertFalse((new UpdateLinkInput(abVariants: []))->isEmpty());
    }

    // ─── createBatch ────────────────────────────────────────────────────────

    public function testCreateBatchPostsToBatchEndpoint(): void
    {
        $this->queueJson($this->batchResultPayload());

        $this->client()->links()->createBatch([
            new BatchLinkInput(targetUrl: 'https://example.com/a'),
            new BatchLinkInput(targetUrl: 'https://example.com/b'),
        ]);

        $request = $this->lastRequest();
        self::assertSame('POST', $request->getMethod());
        self::assertSame('/v1/links/batch', $request->getUri()->getPath());
    }

    public function testCreateBatchSerializesLinksArray(): void
    {
        $this->queueJson($this->batchResultPayload());

        $this->client()->links()->createBatch([
            new BatchLinkInput(
                targetUrl: 'https://example.com/a',
                code: 'a',
                expiresAt: 1_900_000_000,
                domainHostname: 'go.brand.com',
            ),
            new BatchLinkInput(targetUrl: 'https://example.com/b'),
        ]);

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('links', $body);
        self::assertSame([
            [
                'target_url' => 'https://example.com/a',
                'code' => 'a',
                'expires_at' => 1_900_000_000,
                'domain_hostname' => 'go.brand.com',
            ],
            ['target_url' => 'https://example.com/b'],
        ], $body['links']);
    }

    public function testCreateBatchParsesResult(): void
    {
        $this->queueJson($this->batchResultPayload());

        $result = $this->client()->links()->createBatch([
            new BatchLinkInput(targetUrl: 'https://example.com/a'),
            new BatchLinkInput(targetUrl: 'https://bad'),
        ]);

        self::assertInstanceOf(BatchCreateLinksResult::class, $result);
        self::assertSame(1, $result->created);
        self::assertSame(2, $result->total);
        self::assertCount(2, $result->results);
        self::assertContainsOnlyInstancesOf(BatchLinkResult::class, $result->results);

        self::assertSame(0, $result->results[0]->index);
        self::assertTrue($result->results[0]->ok);
        self::assertSame('aaa', $result->results[0]->code);
        self::assertNull($result->results[0]->error);

        self::assertSame(1, $result->results[1]->index);
        self::assertFalse($result->results[1]->ok);
        self::assertNull($result->results[1]->code);
        self::assertSame('bad_target_url', $result->results[1]->error);
    }

    public function testCreateBatchErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'rate_limited', 'message' => 'slow down'], 429);

        $this->expectException(\Rerout\Exceptions\ReroutException::class);
        $this->client()->links()->createBatch([
            new BatchLinkInput(targetUrl: 'https://example.com/a'),
        ]);
    }

    // ─── Fixtures ───────────────────────────────────────────────────────────

    /**
     * @return array<string, mixed>
     */
    private function classicLinkPayload(): array
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
            'tags' => [],
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function smartLinkPayload(): array
    {
        return array_merge($this->classicLinkPayload(), [
            'password_protected' => true,
            'max_clicks' => 1000,
            'click_count' => 42,
            'track_conversions' => true,
            'routing_rules' => [
                [
                    'condition_type' => 'country',
                    'condition_op' => 'is',
                    'condition_value' => 'ZA',
                    'target_url' => 'https://example.com/za',
                ],
            ],
            'ab_variants' => [
                ['id' => 1, 'target_url' => 'https://example.com/a', 'weight' => 70],
                ['id' => 2, 'target_url' => 'https://example.com/b', 'weight' => 30],
            ],
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    private function batchResultPayload(): array
    {
        return [
            'created' => 1,
            'total' => 2,
            'results' => [
                ['index' => 0, 'ok' => true, 'code' => 'aaa'],
                ['index' => 1, 'ok' => false, 'error' => 'bad_target_url'],
            ],
        ];
    }
}
