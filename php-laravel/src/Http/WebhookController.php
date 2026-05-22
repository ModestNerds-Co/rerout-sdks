<?php

declare(strict_types=1);

namespace Rerout\Laravel\Http;

use Illuminate\Contracts\Config\Repository as ConfigRepository;
use Illuminate\Contracts\Events\Dispatcher;
use Illuminate\Http\Request;
use Illuminate\Http\Response;
use Rerout\Laravel\Events\DomainFailed;
use Rerout\Laravel\Events\LinkClicked;
use Rerout\Laravel\Events\QrScanned;
use Rerout\Webhooks\SignatureVerifier;

/**
 * Receives Rerout webhook deliveries.
 *
 * The flow:
 *
 * 1. Verify the `X-Rerout-Signature` header against the raw request body using
 *    the configured signing secret. A missing/invalid signature → `401`.
 * 2. Decode the JSON body. A non-JSON or non-object body → `400`.
 * 3. Map the `type` field to a Laravel event and dispatch it.
 * 4. Return `200`.
 *
 * Wire it up via the bundled route (enabled by default) or point your own
 * route at `WebhookController::class` as an invokable controller.
 */
final class WebhookController
{
    /**
     * Map of Rerout webhook `type` values to the events they dispatch.
     *
     * @var array<string, class-string>
     */
    private const EVENT_MAP = [
        'link.clicked' => LinkClicked::class,
        'qr.scanned' => QrScanned::class,
        'domain.failed' => DomainFailed::class,
    ];

    public function __construct(
        private readonly ConfigRepository $config,
        private readonly Dispatcher $events,
    ) {
    }

    /**
     * Handle an inbound webhook request.
     */
    public function __invoke(Request $request): Response
    {
        $rawBody = $request->getContent();
        $signatureHeader = $request->header('X-Rerout-Signature');
        $signature = is_string($signatureHeader) ? $signatureHeader : '';

        $secret = $this->config->get('rerout.webhook.secret');
        $secret = is_string($secret) ? $secret : '';

        $tolerance = $this->config->get('rerout.webhook.tolerance');
        $tolerance = is_int($tolerance) ? $tolerance : SignatureVerifier::DEFAULT_TOLERANCE_SECONDS;

        $verified = SignatureVerifier::verify(
            rawBody: $rawBody,
            signatureHeader: $signature,
            secret: $secret,
            toleranceSeconds: $tolerance,
        );

        if (!$verified) {
            return new Response('invalid signature', Response::HTTP_UNAUTHORIZED);
        }

        $decoded = json_decode($rawBody, true);
        if (!is_array($decoded)) {
            return new Response('invalid payload', Response::HTTP_BAD_REQUEST);
        }

        /** @var array<string, mixed> $payload */
        $payload = $decoded;

        $type = $payload['type'] ?? null;
        if (is_string($type) && isset(self::EVENT_MAP[$type])) {
            $eventClass = self::EVENT_MAP[$type];
            $this->events->dispatch(new $eventClass($payload));
        }

        return new Response('', Response::HTTP_OK);
    }
}
