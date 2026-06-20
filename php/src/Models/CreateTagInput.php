<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * Request body for `POST /v1/projects/me/tags`.
 *
 * `name` is required. `color` is optional — when omitted (null) it is stripped
 * from the outgoing payload and the server applies its default (`teal`).
 */
final readonly class CreateTagInput
{
    /**
     * @param string      $name  Human-readable tag name. Required.
     * @param string|null $color Display colour. Optional; server validates and defaults to `teal`.
     */
    public function __construct(
        public string $name,
        public ?string $color = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects. A null `color` is
     * stripped so an unset field doesn't override the server default.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = ['name' => $this->name];
        if ($this->color !== null) {
            $out['color'] = $this->color;
        }

        return $out;
    }
}
