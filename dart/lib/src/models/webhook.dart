//
//  rerout
//  webhook.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A webhook endpoint registered to the project, as returned by the Rerout
/// API. Mirrors the server-side `WebhookEndpointResponse` shape.
@immutable
class Webhook {
  /// Creates a [Webhook].
  const Webhook({
    required this.id,
    required this.projectId,
    required this.name,
    required this.url,
    required this.events,
    required this.isActive,
    required this.payloadFormat,
    required this.createdAt,
    required this.updatedAt,
    this.lastDeliveryAt,
    this.lastSuccessAt,
    this.lastFailureAt,
  });

  /// Parses a [Webhook] from the API JSON envelope.
  factory Webhook.fromJson(Map<String, dynamic> json) => Webhook(
    id: json['id'] as String,
    projectId: json['project_id'] as String,
    name: json['name'] as String,
    url: json['url'] as String,
    events:
        (json['events'] as List<dynamic>? ?? const <dynamic>[])
            .map((e) => e as String)
            .toList(growable: false),
    isActive: json['is_active'] as bool,
    payloadFormat: json['payload_format'] as String,
    createdAt: json['created_at'] as int,
    updatedAt: json['updated_at'] as int,
    lastDeliveryAt: json['last_delivery_at'] as int?,
    lastSuccessAt: json['last_success_at'] as int?,
    lastFailureAt: json['last_failure_at'] as int?,
  );

  /// Stable endpoint identifier — looks like `wh_…`.
  final String id;

  /// Project that owns the endpoint.
  final String projectId;

  /// Human-readable label for the endpoint.
  final String name;

  /// Public `https://` URL that receives signed POST deliveries.
  final String url;

  /// Event types this endpoint is subscribed to (e.g. `link.created`).
  final List<String> events;

  /// Whether the endpoint is currently active.
  final bool isActive;

  /// Payload encoding — `json` or `slack`.
  final String payloadFormat;

  /// Unix seconds — endpoint creation time.
  final int createdAt;

  /// Unix seconds — last mutation.
  final int updatedAt;

  /// Unix seconds — last delivery attempt, or null if none yet.
  final int? lastDeliveryAt;

  /// Unix seconds — last successful delivery, or null if none yet.
  final int? lastSuccessAt;

  /// Unix seconds — last failed delivery, or null if none yet.
  final int? lastFailureAt;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Webhook &&
          id == other.id &&
          projectId == other.projectId &&
          name == other.name &&
          url == other.url &&
          _eventsEqual(events, other.events) &&
          isActive == other.isActive &&
          payloadFormat == other.payloadFormat &&
          createdAt == other.createdAt &&
          updatedAt == other.updatedAt &&
          lastDeliveryAt == other.lastDeliveryAt &&
          lastSuccessAt == other.lastSuccessAt &&
          lastFailureAt == other.lastFailureAt;

  @override
  int get hashCode => Object.hash(
    id,
    projectId,
    name,
    url,
    Object.hashAll(events),
    isActive,
    payloadFormat,
    createdAt,
    updatedAt,
    lastDeliveryAt,
    lastSuccessAt,
    lastFailureAt,
  );

  @override
  String toString() => 'Webhook(id: $id, url: $url)';
}

bool _eventsEqual(List<String> a, List<String> b) {
  if (identical(a, b)) return true;
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
