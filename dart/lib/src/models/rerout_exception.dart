//
//  rerout
//  rerout_exception.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Exception thrown for any failed Rerout API call — bad request, auth issue,
/// rate-limit, or network-level failure.
///
/// The [code] field carries the stable string identifier returned by the
/// Rerout API (for example `bad_target_url`, `rate_limited`, `not_found`) so
/// callers can branch on it without parsing the human-readable [message].
///
/// For network or non-JSON failures the [code] is one of the synthetic
/// client-side values: `network_error`, `timeout`, `unexpected_response`,
/// `unauthorized`, `forbidden`, `not_found`, `rate_limited`, `server_error`,
/// `client_error`.
class ReroutException implements Exception {
  /// Creates a Rerout exception.
  const ReroutException({
    required this.message,
    required this.statusCode,
    required this.code,
    this.path,
    this.timestamp,
  });

  /// The error message — either from the API or a synthesized client message.
  final String message;

  /// The HTTP status code, or `0` when the request never reached the server.
  final int statusCode;

  /// Stable error code, from the API or synthesized client-side.
  final String code;

  /// The API path that caused the error, when known.
  final String? path;

  /// The timestamp of the error (ISO 8601), when supplied by the API.
  final String? timestamp;

  /// `true` when the failure is HTTP 5xx — a server-side issue.
  bool get isServerError => statusCode >= 500 && statusCode < 600;

  /// `true` when the failure is HTTP 429 — caller should back off and retry.
  bool get isRateLimited => statusCode == 429;

  @override
  String toString() {
    return 'ReroutException(code: $code, statusCode: $statusCode, '
        'message: $message, path: $path, timestamp: $timestamp)';
  }
}
