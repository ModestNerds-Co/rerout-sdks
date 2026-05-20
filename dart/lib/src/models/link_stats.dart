//
//  rerout
//  link_stats.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/stats_breakdown.dart';

/// Analytics for a single short link across the requested window.
@immutable
class LinkStats {
  /// Creates [LinkStats].
  const LinkStats({
    required this.code,
    required this.days,
    required this.totalClicks,
    required this.qrScans,
    required this.countries,
    required this.referrers,
  });

  /// Parses [LinkStats] from the API JSON envelope.
  factory LinkStats.fromJson(Map<String, dynamic> json) => LinkStats(
    code: json['code'] as String? ?? '',
    days: (json['days'] as num?)?.toInt() ?? 0,
    totalClicks: (json['total_clicks'] as num?)?.toInt() ?? 0,
    qrScans: (json['qr_scans'] as num?)?.toInt() ?? 0,
    countries:
        (json['countries'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<Map<String, dynamic>>()
            .map(StatsBreakdown.fromJson)
            .toList(growable: false),
    referrers:
        (json['referrers'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<Map<String, dynamic>>()
            .map(StatsBreakdown.fromJson)
            .toList(growable: false),
  );

  /// The short code these stats belong to.
  final String code;

  /// Window size in days the totals span.
  final int days;

  /// Total clicks in the window.
  final int totalClicks;

  /// Subset of [totalClicks] attributed to a QR scan.
  final int qrScans;

  /// Top countries.
  final List<StatsBreakdown> countries;

  /// Top referrers.
  final List<StatsBreakdown> referrers;
}
