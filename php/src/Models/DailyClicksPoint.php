<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A daily point in the project stats series.
 */
final readonly class DailyClicksPoint
{
    public function __construct(
        public int $day,
        public int $clicks,
        public int $qrScans,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $day = $data['day'] ?? null;
        $clicks = $data['clicks'] ?? null;
        $qrScans = $data['qr_scans'] ?? null;

        if (!is_int($day) || !is_int($clicks) || !is_int($qrScans)) {
            throw new InvalidArgumentException('Expected int fields day/clicks/qr_scans in DailyClicksPoint payload.');
        }

        return new self($day, $clicks, $qrScans);
    }
}
