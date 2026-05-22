<?php

declare(strict_types=1);

namespace Rerout\Laravel\Events;

use Illuminate\Foundation\Events\Dispatchable;

/**
 * Fired when Rerout delivers a `qr.scanned` webhook.
 */
final class QrScanned
{
    use Dispatchable;

    /**
     * @param array<string, mixed> $payload The decoded webhook event body.
     */
    public function __construct(public readonly array $payload)
    {
    }
}
