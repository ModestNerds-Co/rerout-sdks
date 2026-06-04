<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A short link as returned by the Rerout API.
 *
 * Fields mirror the server-side `LinkResponse` shape one-to-one so JSON is
 * parsed without transformation.
 */
final readonly class Link
{
    /**
     * @param string      $code            Short link path code.
     * @param string      $shortUrl        Fully-qualified short URL — `https://{host}/{code}`.
     * @param string|null $domainHostname  Verified custom domain hosting this link, when bound.
     * @param string      $targetUrl       Destination the redirect resolves to.
     * @param string      $projectId       Project that owns the link.
     * @param int|null    $expiresAt       Unix seconds — expiration. Null for permanent links.
     * @param bool        $isActive        Whether the link is currently active.
     * @param string|null $seoTitle        Override social preview title.
     * @param string|null $seoDescription  Override social preview description.
     * @param string|null $seoImageUrl     Override social preview image URL.
     * @param string|null $seoCanonicalUrl Override preview canonical URL.
     * @param bool        $seoNoindex      Whether the preview HTML should be indexed.
     * @param int|null    $seoUpdatedAt    Unix seconds — last SEO field change.
     * @param int         $createdAt       Unix seconds — link creation time.
     * @param int                $updatedAt         Unix seconds — last mutation.
     * @param list<Tag>          $tags              Tags attached to the link. Read-only;
     *                                              empty when none are bound.
     * @param bool               $passwordProtected Whether the link requires a password.
     * @param int|null           $maxClicks         Click cap before the link stops resolving. Null for unlimited.
     * @param int                $clickCount        Total clicks recorded so far.
     * @param bool               $trackConversions  Whether conversion tracking is enabled.
     * @param list<RoutingRule>  $routingRules      Smart-routing rules evaluated in order.
     * @param list<AbVariant>    $abVariants        A/B test variants traffic is split across.
     */
    public function __construct(
        public string $code,
        public string $shortUrl,
        public ?string $domainHostname,
        public string $targetUrl,
        public string $projectId,
        public ?int $expiresAt,
        public bool $isActive,
        public ?string $seoTitle,
        public ?string $seoDescription,
        public ?string $seoImageUrl,
        public ?string $seoCanonicalUrl,
        public bool $seoNoindex,
        public ?int $seoUpdatedAt,
        public int $createdAt,
        public int $updatedAt,
        public array $tags = [],
        public bool $passwordProtected = false,
        public ?int $maxClicks = null,
        public int $clickCount = 0,
        public bool $trackConversions = false,
        public array $routingRules = [],
        public array $abVariants = [],
    ) {
    }

    /**
     * Parse a {@see Link} from an API JSON envelope.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            code: self::requireString($data, 'code'),
            shortUrl: self::requireString($data, 'short_url'),
            domainHostname: self::nullableString($data, 'domain_hostname'),
            targetUrl: self::requireString($data, 'target_url'),
            projectId: self::requireString($data, 'project_id'),
            expiresAt: self::nullableInt($data, 'expires_at'),
            isActive: self::requireBool($data, 'is_active'),
            seoTitle: self::nullableString($data, 'seo_title'),
            seoDescription: self::nullableString($data, 'seo_description'),
            seoImageUrl: self::nullableString($data, 'seo_image_url'),
            seoCanonicalUrl: self::nullableString($data, 'seo_canonical_url'),
            seoNoindex: self::optionalBool($data, 'seo_noindex', true),
            seoUpdatedAt: self::nullableInt($data, 'seo_updated_at'),
            createdAt: self::requireInt($data, 'created_at'),
            updatedAt: self::requireInt($data, 'updated_at'),
            tags: self::mapTags($data['tags'] ?? []),
            passwordProtected: self::optionalBool($data, 'password_protected', false),
            maxClicks: self::nullableInt($data, 'max_clicks'),
            clickCount: self::optionalInt($data, 'click_count', 0),
            trackConversions: self::optionalBool($data, 'track_conversions', false),
            routingRules: self::mapRoutingRules($data['routing_rules'] ?? []),
            abVariants: self::mapAbVariants($data['ab_variants'] ?? []),
        );
    }

    /**
     * @return list<Tag>
     */
    private static function mapTags(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_array($row)) {
                /** @var array<string, mixed> $row */
                $out[] = Tag::fromArray($row);
            }
        }

        return $out;
    }

    /**
     * @return list<RoutingRule>
     */
    private static function mapRoutingRules(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_array($row)) {
                /** @var array<string, mixed> $row */
                $out[] = RoutingRule::fromArray($row);
            }
        }

        return $out;
    }

    /**
     * @return list<AbVariant>
     */
    private static function mapAbVariants(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_array($row)) {
                /** @var array<string, mixed> $row */
                $out[] = AbVariant::fromArray($row);
            }
        }

        return $out;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireString(array $data, string $key): string
    {
        $value = $data[$key] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function nullableString(array $data, string $key): ?string
    {
        $value = $data[$key] ?? null;
        if ($value === null) {
            return null;
        }
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected nullable string field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireInt(array $data, string $key): int
    {
        $value = $data[$key] ?? null;
        if (!is_int($value)) {
            throw new InvalidArgumentException("Expected int field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function nullableInt(array $data, string $key): ?int
    {
        $value = $data[$key] ?? null;
        if ($value === null) {
            return null;
        }
        if (!is_int($value)) {
            throw new InvalidArgumentException("Expected nullable int field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireBool(array $data, string $key): bool
    {
        $value = $data[$key] ?? null;
        if (!is_bool($value)) {
            throw new InvalidArgumentException("Expected bool field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function optionalBool(array $data, string $key, bool $default): bool
    {
        $value = $data[$key] ?? null;
        if ($value === null) {
            return $default;
        }
        if (!is_bool($value)) {
            throw new InvalidArgumentException("Expected bool field '{$key}' in Link payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function optionalInt(array $data, string $key, int $default): int
    {
        $value = $data[$key] ?? null;
        if ($value === null) {
            return $default;
        }
        if (!is_int($value)) {
            throw new InvalidArgumentException("Expected int field '{$key}' in Link payload.");
        }

        return $value;
    }
}
