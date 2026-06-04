<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * Request body for `POST /v1/links`.
 *
 * Only `target_url` is required. Unset (null) fields are stripped from the
 * outgoing payload — pass an explicit value or omit the named argument
 * entirely.
 */
final readonly class CreateLinkInput
{
    /**
     * @param string      $targetUrl       Absolute `https://` destination URL. Max 2048 characters.
     * @param string|null $domainHostname  Verified custom domain to host this link on. Omit for `rerout.co/:code`.
     * @param string|null $code            Custom path. Only valid when `domainHostname` is provided.
     * @param int|null    $expiresAt       Unix seconds — expiration. Omit for a permanent link.
     * @param string|null $seoTitle        Override social preview title. Max 90 characters.
     * @param string|null $seoDescription  Override social preview description. Max 220 characters.
     * @param string|null $seoImageUrl     Absolute `https://` social preview image URL.
     * @param string|null $seoCanonicalUrl Canonical URL for the preview HTML.
     * @param bool|null   $seoNoindex      Whether the preview page should be marked noindex.
     * @param string|null $password        Smart Link password. Visitors must enter it before redirecting.
     * @param int|null    $maxClicks       Click cap before the link stops resolving.
     * @param bool|null   $trackConversions Whether to enable conversion tracking for this link.
     * @param list<RoutingRule>|null  $routingRules Smart-routing rules evaluated in order.
     * @param list<AbVariantInput>|null $abVariants A/B test variants to split traffic across.
     */
    public function __construct(
        public string $targetUrl,
        public ?string $domainHostname = null,
        public ?string $code = null,
        public ?int $expiresAt = null,
        public ?string $seoTitle = null,
        public ?string $seoDescription = null,
        public ?string $seoImageUrl = null,
        public ?string $seoCanonicalUrl = null,
        public ?bool $seoNoindex = null,
        public ?string $password = null,
        public ?int $maxClicks = null,
        public ?bool $trackConversions = null,
        public ?array $routingRules = null,
        public ?array $abVariants = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects. Null fields are
     * stripped so an unset field doesn't accidentally clear server state.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = ['target_url' => $this->targetUrl];
        if ($this->domainHostname !== null) {
            $out['domain_hostname'] = $this->domainHostname;
        }
        if ($this->code !== null) {
            $out['code'] = $this->code;
        }
        if ($this->expiresAt !== null) {
            $out['expires_at'] = $this->expiresAt;
        }
        if ($this->seoTitle !== null) {
            $out['seo_title'] = $this->seoTitle;
        }
        if ($this->seoDescription !== null) {
            $out['seo_description'] = $this->seoDescription;
        }
        if ($this->seoImageUrl !== null) {
            $out['seo_image_url'] = $this->seoImageUrl;
        }
        if ($this->seoCanonicalUrl !== null) {
            $out['seo_canonical_url'] = $this->seoCanonicalUrl;
        }
        if ($this->seoNoindex !== null) {
            $out['seo_noindex'] = $this->seoNoindex;
        }
        if ($this->password !== null) {
            $out['password'] = $this->password;
        }
        if ($this->maxClicks !== null) {
            $out['max_clicks'] = $this->maxClicks;
        }
        if ($this->trackConversions !== null) {
            $out['track_conversions'] = $this->trackConversions;
        }
        if ($this->routingRules !== null) {
            $out['routing_rules'] = array_map(
                static fn (RoutingRule $rule): array => $rule->toArray(),
                array_values($this->routingRules),
            );
        }
        if ($this->abVariants !== null) {
            $out['ab_variants'] = array_map(
                static fn (AbVariantInput $variant): array => $variant->toArray(),
                array_values($this->abVariants),
            );
        }

        return $out;
    }
}
