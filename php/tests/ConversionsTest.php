<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use Rerout\Exceptions\ReroutException;
use Rerout\Models\RecordConversionInput;
use Rerout\Models\RecordedConversion;
use Rerout\Resources\Conversions;

/**
 * Coverage for the {@see Conversions} namespace (`record`) against a mocked
 * HTTP layer.
 */
#[CoversClass(Conversions::class)]
#[CoversClass(RecordConversionInput::class)]
#[CoversClass(RecordedConversion::class)]
final class ConversionsTest extends TestCase
{
    public function testRecordPostsToConversionsEndpoint(): void
    {
        $this->queueJson(['recorded' => true, 'duplicate' => false]);

        $result = $this->client()->conversions()->record(new RecordConversionInput(
            clickId: 'clk_123',
            eventName: 'purchase',
        ));

        $request = $this->lastRequest();
        self::assertSame('POST', $request->getMethod());
        self::assertSame('/v1/conversions', $request->getUri()->getPath());

        self::assertInstanceOf(RecordedConversion::class, $result);
        self::assertTrue($result->recorded);
        self::assertFalse($result->duplicate);
    }

    public function testRecordSendsRequiredFieldsOnly(): void
    {
        $this->queueJson(['recorded' => true, 'duplicate' => false]);

        $this->client()->conversions()->record(new RecordConversionInput(
            clickId: 'clk_123',
            eventName: 'signup',
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame([
            'click_id' => 'clk_123',
            'event_name' => 'signup',
        ], $body);
        self::assertArrayNotHasKey('value_cents', $body);
        self::assertArrayNotHasKey('currency', $body);
    }

    public function testRecordForwardsValueAndCurrencyWhenProvided(): void
    {
        $this->queueJson(['recorded' => true, 'duplicate' => false]);

        $this->client()->conversions()->record(new RecordConversionInput(
            clickId: 'clk_123',
            eventName: 'purchase',
            valueCents: 4999,
            currency: 'USD',
        ));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(4999, $body['value_cents']);
        self::assertSame('USD', $body['currency']);
    }

    public function testRecordParsesDuplicateFlag(): void
    {
        $this->queueJson(['recorded' => true, 'duplicate' => true]);

        $result = $this->client()->conversions()->record(new RecordConversionInput(
            clickId: 'clk_123',
            eventName: 'purchase',
        ));

        self::assertTrue($result->recorded);
        self::assertTrue($result->duplicate);
    }

    public function testRecordErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'unknown click'], 404);

        try {
            $this->client()->conversions()->record(new RecordConversionInput(
                clickId: 'clk_missing',
                eventName: 'purchase',
            ));
            self::fail('Expected ReroutException.');
        } catch (ReroutException $e) {
            self::assertSame('not_found', $e->code());
            self::assertSame(404, $e->status());
        }
    }
}
