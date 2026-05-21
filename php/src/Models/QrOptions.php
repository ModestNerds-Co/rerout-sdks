<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * QR rendering options for `Rerout::qr()->url()` and `svg()`.
 *
 * Every field is optional — omit any to fall back to the server default.
 */
final readonly class QrOptions
{
    /**
     * @param int|null         $size    Module size in pixels. 1–32. Server default: 8.
     * @param int|null         $margin  Quiet-zone modules. 0–16. Server default: 4.
     * @param string|null      $ecc     Error-correction level — `L`, `M`, `Q`, or `H`.
     * @param string|null      $domain  Force the QR to encode a specific verified custom domain.
     * @param bool|string|null $refresh Cache-bust token. `true` emits `refresh=1`; any non-empty
     *                                  string is forwarded verbatim.
     */
    public function __construct(
        public ?int $size = null,
        public ?int $margin = null,
        public ?string $ecc = null,
        public ?string $domain = null,
        public bool|string|null $refresh = null,
    ) {
    }

    /**
     * Render this options bag into URL query pairs.
     *
     * @return array<string, string>
     */
    public function toQueryParameters(): array
    {
        $out = [];
        if ($this->size !== null) {
            $out['size'] = (string) $this->size;
        }
        if ($this->margin !== null) {
            $out['margin'] = (string) $this->margin;
        }
        if ($this->ecc !== null) {
            $out['ecc'] = $this->ecc;
        }
        if ($this->domain !== null) {
            $out['domain'] = $this->domain;
        }
        if ($this->refresh !== null) {
            $out['refresh'] = $this->refresh === true ? '1' : (string) $this->refresh;
        }

        return $out;
    }
}
