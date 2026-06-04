<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * Request body for `POST /v1/conversions`.
 *
 * `clickId` and `eventName` are required. Optional fields left null are
 * stripped from the outgoing payload.
 */
final readonly class RecordConversionInput
{
    /**
     * @param string      $clickId    Identifier of the click that led to the conversion.
     * @param string      $eventName  Name of the conversion event (e.g. `purchase`).
     * @param int|null    $valueCents Monetary value of the conversion, in cents.
     * @param string|null $currency   ISO 4217 currency code for `valueCents`.
     */
    public function __construct(
        public string $clickId,
        public string $eventName,
        public ?int $valueCents = null,
        public ?string $currency = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects. Null optional
     * fields are stripped.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = [
            'click_id' => $this->clickId,
            'event_name' => $this->eventName,
        ];
        if ($this->valueCents !== null) {
            $out['value_cents'] = $this->valueCents;
        }
        if ($this->currency !== null) {
            $out['currency'] = $this->currency;
        }

        return $out;
    }
}
