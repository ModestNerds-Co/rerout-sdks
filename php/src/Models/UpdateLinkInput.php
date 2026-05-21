<?php

declare(strict_types=1);

namespace Rerout\Models;

use Rerout\Exceptions\ReroutException;

/**
 * Request body for `PATCH /v1/links/:code`.
 *
 * The shape distinguishes three states for every nullable field:
 *
 * 1. **Leave alone** — argument omitted (defaults to {@see UpdateLinkInput::UNSET}).
 *    The field is not sent in the JSON body and the server keeps its current
 *    value.
 * 2. **Set value** — pass a concrete value (string, int, bool …).
 * 3. **Clear field** — pass {@see UpdateLinkInput::CLEAR}, which serialises as
 *    explicit `null` so the server wipes the existing value.
 *
 * Both `UNSET` and `CLEAR` are referenced via the public constants
 * {@see UpdateLinkInput::UNSET} and {@see UpdateLinkInput::CLEAR} (strings
 * with sentinel-marker prefixes so they cannot collide with real values).
 *
 * Sending a payload with no fields throws a client-side {@see ReroutException}
 * — the API rejects empty PATCH bodies and there's no reason to round-trip.
 */
final class UpdateLinkInput
{
    /**
     * Sentinel meaning "do not include this field in the request body".
     * Default for every named argument. Use {@see UpdateLinkInput::CLEAR}
     * to explicitly null a field server-side.
     */
    public const string UNSET = "\0__rerout_unset__\0";

    /**
     * Sentinel meaning "explicitly set this field to null on the server".
     * Pass as the value for any nullable field. Required to distinguish
     * "leave alone" from "wipe".
     */
    public const string CLEAR = "\0__rerout_clear__\0";

    public function __construct(
        public readonly string $targetUrl = self::UNSET,
        public readonly int|string|null $expiresAt = self::UNSET,
        public readonly bool|string $isActive = self::UNSET,
        public readonly ?string $seoTitle = self::UNSET,
        public readonly ?string $seoDescription = self::UNSET,
        public readonly ?string $seoImageUrl = self::UNSET,
        public readonly ?string $seoCanonicalUrl = self::UNSET,
        public readonly bool|string $seoNoindex = self::UNSET,
    ) {
    }

    /**
     * Render the JSON-serialisable shape the API expects. Unset fields are
     * stripped; cleared fields serialise as explicit `null`.
     *
     * @throws ReroutException When the payload would be empty.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = [];

        if ($this->targetUrl !== self::UNSET) {
            $out['target_url'] = $this->targetUrl === self::CLEAR ? null : $this->targetUrl;
        }
        if ($this->expiresAt !== self::UNSET) {
            $out['expires_at'] = $this->expiresAt === self::CLEAR ? null : $this->expiresAt;
        }
        if ($this->isActive !== self::UNSET) {
            $out['is_active'] = $this->isActive === self::CLEAR ? null : $this->isActive;
        }
        if ($this->seoTitle !== self::UNSET) {
            $out['seo_title'] = $this->seoTitle === self::CLEAR ? null : $this->seoTitle;
        }
        if ($this->seoDescription !== self::UNSET) {
            $out['seo_description'] = $this->seoDescription === self::CLEAR ? null : $this->seoDescription;
        }
        if ($this->seoImageUrl !== self::UNSET) {
            $out['seo_image_url'] = $this->seoImageUrl === self::CLEAR ? null : $this->seoImageUrl;
        }
        if ($this->seoCanonicalUrl !== self::UNSET) {
            $out['seo_canonical_url'] = $this->seoCanonicalUrl === self::CLEAR ? null : $this->seoCanonicalUrl;
        }
        if ($this->seoNoindex !== self::UNSET) {
            $out['seo_noindex'] = $this->seoNoindex === self::CLEAR ? null : $this->seoNoindex;
        }

        if ($out === []) {
            throw new ReroutException(
                errorCode: 'bad_request',
                message: 'UpdateLinkInput has no fields to send.',
                status: 0,
            );
        }

        return $out;
    }

    /**
     * True when no field has been set — used internally so the client can
     * short-circuit before hitting the API.
     */
    public function isEmpty(): bool
    {
        return $this->targetUrl === self::UNSET
            && $this->expiresAt === self::UNSET
            && $this->isActive === self::UNSET
            && $this->seoTitle === self::UNSET
            && $this->seoDescription === self::UNSET
            && $this->seoImageUrl === self::UNSET
            && $this->seoCanonicalUrl === self::UNSET
            && $this->seoNoindex === self::UNSET;
    }
}
