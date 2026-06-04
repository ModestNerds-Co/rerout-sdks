//
//  rerout
//  recorded_conversion.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// The result of recording a conversion.
@immutable
class RecordedConversion {
  /// Creates a [RecordedConversion].
  const RecordedConversion({required this.recorded, required this.duplicate});

  /// Parses a [RecordedConversion] from the API JSON envelope.
  factory RecordedConversion.fromJson(Map<String, dynamic> json) =>
      RecordedConversion(
        recorded: json['recorded'] as bool? ?? false,
        duplicate: json['duplicate'] as bool? ?? false,
      );

  /// Whether the conversion is now recorded.
  final bool recorded;

  /// Whether this `(clickId, eventName)` was already recorded (idempotent).
  final bool duplicate;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is RecordedConversion &&
          recorded == other.recorded &&
          duplicate == other.duplicate;

  @override
  int get hashCode => Object.hash(recorded, duplicate);

  @override
  String toString() =>
      'RecordedConversion(recorded: $recorded, duplicate: $duplicate)';
}
