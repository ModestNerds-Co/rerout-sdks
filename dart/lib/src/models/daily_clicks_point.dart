//
//  rerout
//  daily_clicks_point.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A single point in a daily clicks time series.
@immutable
class DailyClicksPoint {
  /// Creates a [DailyClicksPoint].
  const DailyClicksPoint({
    required this.day,
    required this.clicks,
    required this.qrScans,
  });

  /// Parses one point from the API JSON envelope.
  factory DailyClicksPoint.fromJson(Map<String, dynamic> json) =>
      DailyClicksPoint(
        day: (json['day'] as num).toInt(),
        clicks: (json['clicks'] as num?)?.toInt() ?? 0,
        qrScans: (json['qr_scans'] as num?)?.toInt() ?? 0,
      );

  /// Day bucket — unix seconds at 00:00 UTC.
  final int day;

  /// Total clicks (link + QR) recorded that day.
  final int clicks;

  /// Subset of [clicks] that came from a QR scan.
  final int qrScans;
}
