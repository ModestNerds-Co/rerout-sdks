//
//  rerout
//  routing_rule.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A geo/device routing rule (Smart Links).
///
/// When the rule's condition matches an incoming request, the redirect
/// resolves to [targetUrl] instead of the link's default destination.
///
/// - [conditionType] is `country` or `device`.
/// - [conditionOp] is `is`, `is_not`, or `in`.
@immutable
class RoutingRule {
  /// Creates a [RoutingRule].
  const RoutingRule({
    required this.conditionType,
    required this.conditionOp,
    required this.conditionValue,
    required this.targetUrl,
  });

  /// Parses a [RoutingRule] from the API JSON envelope.
  factory RoutingRule.fromJson(Map<String, dynamic> json) => RoutingRule(
    conditionType: json['condition_type'] as String,
    conditionOp: json['condition_op'] as String,
    conditionValue: json['condition_value'] as String,
    targetUrl: json['target_url'] as String,
  );

  /// What to match against — `country` or `device`.
  final String conditionType;

  /// Comparison operator — `is`, `is_not`, or `in`.
  final String conditionOp;

  /// Value(s) to compare against (e.g. `ZA`, `US,GB`, `mobile`).
  final String conditionValue;

  /// Destination when the rule matches.
  final String targetUrl;

  /// Converts this rule to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'condition_type': conditionType,
    'condition_op': conditionOp,
    'condition_value': conditionValue,
    'target_url': targetUrl,
  };

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is RoutingRule &&
          conditionType == other.conditionType &&
          conditionOp == other.conditionOp &&
          conditionValue == other.conditionValue &&
          targetUrl == other.targetUrl;

  @override
  int get hashCode =>
      Object.hash(conditionType, conditionOp, conditionValue, targetUrl);

  @override
  String toString() =>
      'RoutingRule($conditionType $conditionOp $conditionValue -> $targetUrl)';
}
