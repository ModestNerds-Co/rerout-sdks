<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * Aggregate stats across every link in the project.
 */
final readonly class ProjectStats
{
    /**
     * @param list<DailyClicksPoint> $daily
     * @param list<StatsBreakdown>   $countries
     * @param list<StatsBreakdown>   $referrers
     * @param list<StatsBreakdown>   $devices
     * @param list<StatsBreakdown>   $browsers
     * @param list<StatsBreakdown>   $topCodes
     */
    public function __construct(
        public int $days,
        public int $totalClicks,
        public int $qrScans,
        public array $daily,
        public array $countries,
        public array $referrers,
        public array $devices,
        public array $browsers,
        public array $topCodes,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $days = $data['days'] ?? null;
        $totalClicks = $data['total_clicks'] ?? null;
        $qrScans = $data['qr_scans'] ?? null;

        if (!is_int($days) || !is_int($totalClicks) || !is_int($qrScans)) {
            throw new InvalidArgumentException('Expected int fields days/total_clicks/qr_scans in ProjectStats payload.');
        }

        return new self(
            days: $days,
            totalClicks: $totalClicks,
            qrScans: $qrScans,
            daily: self::mapDaily($data['daily'] ?? []),
            countries: self::mapBreakdowns($data['countries'] ?? []),
            referrers: self::mapBreakdowns($data['referrers'] ?? []),
            devices: self::mapBreakdowns($data['devices'] ?? []),
            browsers: self::mapBreakdowns($data['browsers'] ?? []),
            topCodes: self::mapBreakdowns($data['top_codes'] ?? []),
        );
    }

    /**
     * @return list<DailyClicksPoint>
     */
    private static function mapDaily(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_array($row)) {
                /** @var array<string, mixed> $row */
                $out[] = DailyClicksPoint::fromArray($row);
            }
        }

        return $out;
    }

    /**
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
