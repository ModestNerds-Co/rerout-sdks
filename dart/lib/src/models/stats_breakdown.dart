//
//  rerout
//  stats_breakdown.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A single bucket in an analytics breakdown — e.g. one country, one device.
@immutable
class StatsBreakdown {
  /// Creates a [StatsBreakdown].
  const StatsBreakdown({required this.value, required this.clicks});

  /// Parses one bucket from the API JSON envelope.
  factory StatsBreakdown.fromJson(Map<String, dynamic> json) => StatsBreakdown(
    value: json['value'] as String? ?? '',
    clicks: (json['clicks'] as num?)?.toInt() ?? 0,
  );

  /// The bucket label — country code, device class, browser name, etc.
  final String value;

  /// Click count for this bucket.
  final int clicks;
}
