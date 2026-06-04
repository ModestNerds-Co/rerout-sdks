//
//  rerout
//  ab_variant.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A weighted A/B destination (Smart Links), as returned by the API.
@immutable
class AbVariant {
  /// Creates an [AbVariant].
  const AbVariant({
    required this.id,
    required this.targetUrl,
    required this.weight,
  });

  /// Parses an [AbVariant] from the API JSON envelope.
  factory AbVariant.fromJson(Map<String, dynamic> json) => AbVariant(
    id: json['id'] as int,
    targetUrl: json['target_url'] as String,
    weight: json['weight'] as int,
  );

  /// Stable variant id assigned by the server.
  final int id;

  /// Destination for this variant.
  final String targetUrl;

  /// Relative weight in the split.
  final int weight;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AbVariant &&
          id == other.id &&
          targetUrl == other.targetUrl &&
          weight == other.weight;

  @override
  int get hashCode => Object.hash(id, targetUrl, weight);

  @override
  String toString() =>
      'AbVariant(id: $id, targetUrl: $targetUrl, weight: $weight)';
}

/// A weighted A/B destination as supplied on create/update.
///
/// Unlike [AbVariant], there is no server-assigned `id`, and [weight] is
/// optional — the server applies a default when omitted.
@immutable
class AbVariantInput {
  /// Creates an [AbVariantInput].
  const AbVariantInput({required this.targetUrl, this.weight});

  /// Destination for this variant.
  final String targetUrl;

  /// Relative weight in the split. Defaults server-side when omitted.
  final int? weight;

  /// Converts this variant to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'target_url': targetUrl,
    if (weight != null) 'weight': weight,
  };

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AbVariantInput &&
          targetUrl == other.targetUrl &&
          weight == other.weight;

  @override
  int get hashCode => Object.hash(targetUrl, weight);

  @override
  String toString() => 'AbVariantInput(targetUrl: $targetUrl, weight: $weight)';
}
