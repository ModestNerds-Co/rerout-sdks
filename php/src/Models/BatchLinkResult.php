<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * The outcome of a single link in a batch create. `index` ties the result back
 * to its position in the submitted `links` array. On success `code` is set; on
 * failure `error` carries the reason.
 */
final readonly class BatchLinkResult
{
    /**
     * @param int         $index Position of this link in the submitted batch.
     * @param bool        $ok    Whether the link was created.
     * @param string|null $code  Created link code, when `ok`.
     * @param string|null $error Failure reason, when not `ok`.
     */
    public function __construct(
        public int $index,
        public bool $ok,
        public ?string $code = null,
        public ?string $error = null,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $index = $data['index'] ?? null;
        if (!is_int($index)) {
            throw new InvalidArgumentException("Expected int field 'index' in BatchLinkResult payload.");
        }

        $ok = $data['ok'] ?? null;
        if (!is_bool($ok)) {
            throw new InvalidArgumentException("Expected bool field 'ok' in BatchLinkResult payload.");
        }

        $code = $data['code'] ?? null;
        if ($code !== null && !is_string($code)) {
            throw new InvalidArgumentException("Expected nullable string field 'code' in BatchLinkResult payload.");
        }

        $error = $data['error'] ?? null;
        if ($error !== null && !is_string($error)) {
            throw new InvalidArgumentException("Expected nullable string field 'error' in BatchLinkResult payload.");
        }

        return new self(index: $index, ok: $ok, code: $code, error: $error);
    }
}
