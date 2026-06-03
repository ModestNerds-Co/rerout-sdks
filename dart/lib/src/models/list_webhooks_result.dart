//
//  rerout
//  list_webhooks_result.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/webhook.dart';

/// The result of listing webhook endpoints.
@immutable
class ListWebhooksResult {
  /// Creates a [ListWebhooksResult].
  const ListWebhooksResult({required this.endpoints, required this.eventTypes});

  /// Parses the listing from the API JSON envelope.
  factory ListWebhooksResult.fromJson(Map<String, dynamic> json) {
    final endpoints =
        (json['endpoints'] as List<dynamic>? ?? const <dynamic>[])
            .map((e) => Webhook.fromJson(e as Map<String, dynamic>))
            .toList(growable: false);
    final eventTypes =
        (json['event_types'] as List<dynamic>? ?? const <dynamic>[])
            .map((e) => e as String)
            .toList(growable: false);
    return ListWebhooksResult(endpoints: endpoints, eventTypes: eventTypes);
  }

  /// The webhook endpoints registered to the project.
  final List<Webhook> endpoints;

  /// Every event type the server can deliver.
  final List<String> eventTypes;
}
