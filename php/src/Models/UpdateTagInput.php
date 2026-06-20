<?php

declare(strict_types=1);

namespace Rerout\Models;

use Rerout\Exceptions\ReroutException;

/**
 * Request body for `PATCH /v1/projects/me/tags/:tag_id`.
 *
 * Both fields are optional. An omitted (null) field is stripped from the
 * outgoing payload and the server leaves that field unchanged. Tags have no
 * nullable columns, so there is no "clear" sentinel — only "leave alone" vs
 * "set to this value".
 *
 * Sending a payload with no fields throws a client-side {@see ReroutException},
 * mirroring {@see UpdateLinkInput} — the API rejects empty PATCH bodies and
 * there's no reason to round-trip.
 */
final readonly class UpdateTagInput
{
    /**
     * @param string|null $name  New tag name. Omitted (null) leaves it unchanged.
     * @param string|null $color New display colour. Omitted (null) leaves it unchanged.
     */
    public function __construct(
        public ?string $name = null,
        public ?string $color = null,
    ) {
    }

    /**
     * Render the JSON-serialisable shape the API expects. Unset (null) fields
     * are stripped so only the provided fields are forwarded.
     *
     * @throws ReroutException When the payload would be empty.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = [];
        if ($this->name !== null) {
            $out['name'] = $this->name;
        }
        if ($this->color !== null) {
            $out['color'] = $this->color;
        }

        if ($out === []) {
            throw new ReroutException(
                errorCode: 'bad_request',
                message: 'UpdateTagInput has no fields to send.',
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
        return $this->name === null && $this->color === null;
    }
}
