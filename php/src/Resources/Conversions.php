<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Models\RecordConversionInput;
use Rerout\Models\RecordedConversion;
use Rerout\Rerout;

/**
 * Conversion tracking namespace. Reached via `Rerout::conversions()`.
 *
 * Records conversion events against a click so attribution and value can be
 * reported back in analytics.
 */
final class Conversions
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /**
     * Record a conversion event for a click. Idempotent — recording the same
     * click + event twice returns `duplicate = true` and keeps the first record.
     */
    public function record(RecordConversionInput $input): RecordedConversion
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'POST',
            path: '/v1/conversions',
            body: $input->toArray(),
        );

        return RecordedConversion::fromArray($response);
    }
}
