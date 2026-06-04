//
//  rerout
//  record_conversion_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Request body for recording a conversion against a prior click.
class RecordConversionRequest {
  /// Creates a [RecordConversionRequest].
  const RecordConversionRequest({
    required this.clickId,
    required this.eventName,
    this.valueCents,
    this.currency,
  });

  /// The click id (`rrid`) minted on the tracked redirect.
  final String clickId;

  /// Conversion event label (e.g. `purchase`, `signup`). Max 64 chars.
  final String eventName;

  /// Optional monetary value in minor units (cents).
  final int? valueCents;

  /// Optional ISO 4217 currency code (e.g. `USD`).
  final String? currency;

  /// Converts this request to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'click_id': clickId,
    'event_name': eventName,
    if (valueCents != null) 'value_cents': valueCents,
    if (currency != null) 'currency': currency,
  };
}
