<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * A single link item in a batch create (`POST /v1/links/batch`).
 *
 * Only `targetUrl` is required. Null optional fields are stripped from the
 * outgoing payload.
 */
final readonly class BatchLinkInput
{
    /**
     * @param string      $targetUrl      Absolute `https://` destination URL.
     * @param string|null $code           Custom path. Only valid with a verified domain.
     * @param int|null    $expiresAt      Unix seconds — expiration. Omit for a permanent link.
     * @param string|null $domainHostname Verified custom domain to host this link on.
     */
    public function __construct(
        public string $targetUrl,
        public ?string $code = null,
        public ?int $expiresAt = null,
        public ?string $domainHostname = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = ['target_url' => $this->targetUrl];
        if ($this->code !== null) {
            $out['code'] = $this->code;
        }
        if ($this->expiresAt !== null) {
            $out['expires_at'] = $this->expiresAt;
        }
        if ($this->domainHostname !== null) {
            $out['domain_hostname'] = $this->domainHostname;
        }

        return $out;
    }
}
