<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * Per-link click stats over a configurable window.
 */
final readonly class LinkStats
{
    /**
     * @param list<StatsBreakdown> $countries
     * @param list<StatsBreakdown> $referrers
     */
    public function __construct(
        public string $code,
        public int $days,
        public int $totalClicks,
        public int $qrScans,
        public array $countries,
        public array $referrers,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $code = $data['code'] ?? null;
        $days = $data['days'] ?? null;
        $totalClicks = $data['total_clicks'] ?? null;
        $qrScans = $data['qr_scans'] ?? null;

        if (!is_string($code)) {
            throw new InvalidArgumentException("Expected string field 'code' in LinkStats payload.");
        }
        if (!is_int($days) || !is_int($totalClicks) || !is_int($qrScans)) {
            throw new InvalidArgumentException('Expected int fields days/total_clicks/qr_scans in LinkStats payload.');
        }

        return new self(
            code: $code,
            days: $days,
            totalClicks: $totalClicks,
            qrScans: $qrScans,
            countries: self::mapBreakdowns($data['countries'] ?? []),
            referrers: self::mapBreakdowns($data['referrers'] ?? []),
        );
    }

    /**
     * @param mixed $raw
     *
     * @return list<StatsBreakdown>
     */
    private static function mapBreakdowns(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_array($row)) {
                /** @var array<string, mixed> $row */
                $out[] = StatsBreakdown::fromArray($row);
            }
        }

        return $out;
    }
}
