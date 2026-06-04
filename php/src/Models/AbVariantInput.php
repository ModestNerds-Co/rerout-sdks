<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * A request item for an A/B test variant on a Smart Link. Used in the
 * `ab_variants` array of {@see CreateLinkInput} and {@see UpdateLinkInput}.
 *
 * Unlike {@see AbVariant} (the response shape) there is no `id` — the server
 * assigns one — and `weight` is optional; omit it to take the server default.
 */
final readonly class AbVariantInput
{
    /**
     * @param string   $targetUrl Destination this variant resolves to.
     * @param int|null  $weight    Relative traffic weight. Omit for the server default.
     */
    public function __construct(
        public string $targetUrl,
        public ?int $weight = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects. A null `weight`
     * is stripped so the server applies its default.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = ['target_url' => $this->targetUrl];
        if ($this->weight !== null) {
            $out['weight'] = $this->weight;
        }

        return $out;
    }
}
