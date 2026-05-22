<?php

declare(strict_types=1);

namespace Rerout\Laravel\Events;

use Illuminate\Foundation\Events\Dispatchable;

/**
 * Fired when Rerout delivers a `domain.failed` webhook — a custom domain
 * failed verification or its certificate could not be issued.
 */
final class DomainFailed
{
    use Dispatchable;

    /**
     * @param array<string, mixed> $payload The decoded webhook event body.
     */
    public function __construct(public readonly array $payload)
    {
    }
}
