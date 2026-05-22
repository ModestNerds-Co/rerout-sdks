<?php

declare(strict_types=1);

namespace Rerout\Laravel\Events;

use Illuminate\Foundation\Events\Dispatchable;

/**
 * Fired when Rerout delivers a `link.clicked` webhook.
 *
 * Listen for it anywhere in your app:
 *
 * ```php
 * Event::listen(LinkClicked::class, function (LinkClicked $event) {
 *     logger()->info('click', $event->payload);
 * });
 * ```
 */
final class LinkClicked
{
    use Dispatchable;

    /**
     * @param array<string, mixed> $payload The decoded webhook event body.
     */
    public function __construct(public readonly array $payload)
    {
    }
}
