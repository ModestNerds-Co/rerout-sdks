//
//  rerout
//  project_stats.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/daily_clicks_point.dart';
import 'package:rerout/src/models/stats_breakdown.dart';

/// Aggregate analytics for a project across the requested window.
@immutable
class ProjectStats {
  /// Creates [ProjectStats].
  const ProjectStats({
    required this.days,
    required this.totalClicks,
    required this.qrScans,
    required this.daily,
    required this.countries,
    required this.referrers,
    required this.devices,
    required this.browsers,
    required this.topCodes,
  });

  /// Parses [ProjectStats] from the API JSON envelope.
  factory ProjectStats.fromJson(Map<String, dynamic> json) => ProjectStats(
    days: (json['days'] as num?)?.toInt() ?? 0,
    totalClicks: (json['total_clicks'] as num?)?.toInt() ?? 0,
    qrScans: (json['qr_scans'] as num?)?.toInt() ?? 0,
    daily: _parseList(json['daily'], DailyClicksPoint.fromJson),
    countries: _parseList(json['countries'], StatsBreakdown.fromJson),
    referrers: _parseList(json['referrers'], StatsBreakdown.fromJson),
    devices: _parseList(json['devices'], StatsBreakdown.fromJson),
    browsers: _parseList(json['browsers'], StatsBreakdown.fromJson),
    topCodes: _parseList(json['top_codes'], StatsBreakdown.fromJson),
  );

  /// Window size in days the totals span.
  final int days;

  /// Total clicks recorded in the window.
  final int totalClicks;

  /// Total QR scans (subset of [totalClicks]) recorded in the window.
  final int qrScans;

  /// One point per day across the window. Gap-filled by the server.
  final List<DailyClicksPoint> daily;

  /// Top countries by click count.
  final List<StatsBreakdown> countries;

  /// Top referrers by click count.
  final List<StatsBreakdown> referrers;

  /// Click share by device class (mobile / desktop / tablet / bot / unknown).
  final List<StatsBreakdown> devices;

  /// Click share by browser or in-app web view.
  final List<StatsBreakdown> browsers;

  /// Top short codes by click count.
  final List<StatsBreakdown> topCodes;
}

List<T> _parseList<T>(
  Object? raw,
  T Function(Map<String, dynamic>) fromJson,
) {
  if (raw is! List) return const [];
  return raw
      .whereType<Map<String, dynamic>>()
      .map(fromJson)
      .toList(growable: false);
}
