//
//  rerout
//  create_webhook_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Request body for creating a webhook endpoint.
class CreateWebhookRequest {
  /// Creates a [CreateWebhookRequest].
  const CreateWebhookRequest({
    required this.name,
    required this.url,
    required this.events,
    this.isActive,
    this.payloadFormat,
  });

  /// Human-readable label for the endpoint.
  final String name;

  /// Public `https://` URL that receives signed POST deliveries.
  final String url;

  /// Event types to subscribe to (e.g. `link.created`). At least one.
  final List<String> events;

  /// Whether the endpoint starts active. Defaults server-side to `true`.
  final bool? isActive;

  /// Payload encoding — `json` or `slack`. Defaults server-side.
  final String? payloadFormat;

  /// Converts this request to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'name': name,
    'url': url,
    'events': events,
    if (isActive != null) 'is_active': isActive,
    if (payloadFormat != null) 'payload_format': payloadFormat,
  };
}
