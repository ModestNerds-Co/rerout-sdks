//
//  rerout
//  created_webhook.dart
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

/// The result of creating a webhook endpoint.
///
/// The [signingSecret] (`whsec_…`) is returned **once** — store it now to
/// verify inbound deliveries; it is never shown again.
@immutable
class CreatedWebhook {
  /// Creates a [CreatedWebhook].
  const CreatedWebhook({required this.endpoint, required this.signingSecret});

  /// Parses a [CreatedWebhook] from the API JSON envelope.
  factory CreatedWebhook.fromJson(Map<String, dynamic> json) => CreatedWebhook(
    endpoint: Webhook.fromJson(json['endpoint'] as Map<String, dynamic>),
    signingSecret: json['signing_secret'] as String,
  );

  /// The newly-created webhook endpoint.
  final Webhook endpoint;

  /// The signing secret (`whsec_…`). Returned once — persist it now.
  final String signingSecret;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is CreatedWebhook &&
          endpoint == other.endpoint &&
          signingSecret == other.signingSecret;

  @override
  int get hashCode => Object.hash(endpoint, signingSecret);

  @override
  String toString() => 'CreatedWebhook(endpoint: $endpoint)';
}
