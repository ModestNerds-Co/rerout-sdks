//
//  rerout
//  webhook_signature.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'dart:convert';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';

/// Helper for verifying inbound `X-Rerout-Signature` headers.
///
/// Rerout signs every webhook delivery as
/// `t={unix_seconds},v1={hex_hmac_sha256}` where the HMAC is computed over
/// `"{timestamp}.{raw_body}"` with the endpoint signing secret as the key.
///
/// ## Usage
///
/// ```dart
/// import 'package:rerout/rerout.dart';
///
/// final ok = ReroutWebhookSignature.verify(
///   rawBody: rawBody,
///   signatureHeader: request.headers['x-rerout-signature']!,
///   secret: dotenv['REROUT_WEBHOOK_SECRET']!,
/// );
/// if (!ok) return Response(401);
/// ```
class ReroutWebhookSignature {
  ReroutWebhookSignature._();

  /// Default tolerance window in seconds between the `t=` timestamp and the
  /// current time. Set to 300 (five minutes) — protects against captured-
  /// replay attacks.
  static const int defaultToleranceSeconds = 300;

  /// Returns `true` when [signatureHeader] is a valid Rerout HMAC signature
  /// for [rawBody] under [secret].
  ///
  /// Returns `false` when the header is missing, malformed, the timestamp is
  /// outside the tolerance window, or the HMAC doesn't match.
  ///
  /// Pass `toleranceSeconds: 0` to disable the timestamp staleness check.
  /// Pass a custom [clock] to make tests deterministic (defaults to
  /// `DateTime.now()`).
  static bool verify({
    required String rawBody,
    required String signatureHeader,
    required String secret,
    int toleranceSeconds = defaultToleranceSeconds,
    DateTime Function()? clock,
  }) {
    if (signatureHeader.isEmpty || secret.isEmpty) return false;
    final parsed = _parseHeader(signatureHeader);
    if (parsed == null) return false;

    if (toleranceSeconds > 0) {
      final now = (clock ?? DateTime.now).call();
      final nowSeconds = now.millisecondsSinceEpoch ~/ 1000;
      if ((nowSeconds - parsed.timestamp).abs() > toleranceSeconds) {
        return false;
      }
    }

    final expected = Hmac(sha256, utf8.encode(secret))
        .convert(utf8.encode('${parsed.timestamp}.$rawBody'))
        .bytes;
    final actual = _hexDecode(parsed.v1);
    if (actual == null || expected.length != actual.length) return false;
    return _constantTimeEquals(expected, actual);
  }

  static _ParsedHeader? _parseHeader(String header) {
    int? timestamp;
    String? v1;
    for (final segment in header.split(',')) {
      final eq = segment.indexOf('=');
      if (eq <= 0) continue;
      final key = segment.substring(0, eq).trim().toLowerCase();
      final value = segment.substring(eq + 1).trim();
      if (key == 't') {
        final parsed = int.tryParse(value);
        if (parsed != null && parsed > 0) timestamp = parsed;
      } else if (key == 'v1') {
        if (value.isNotEmpty) v1 = value;
      }
    }
    if (timestamp == null || v1 == null) return null;
    return _ParsedHeader(timestamp, v1);
  }

  static Uint8List? _hexDecode(String hex) {
    if (hex.isEmpty || hex.length.isOdd) return null;
    final out = Uint8List(hex.length ~/ 2);
    for (var i = 0; i < out.length; i++) {
      final hi = _hexDigit(hex.codeUnitAt(i * 2));
      final lo = _hexDigit(hex.codeUnitAt(i * 2 + 1));
      if (hi < 0 || lo < 0) return null;
      out[i] = (hi << 4) | lo;
    }
    return out;
  }

  static int _hexDigit(int charCode) {
    // 0-9
    if (charCode >= 0x30 && charCode <= 0x39) return charCode - 0x30;
    // A-F
    if (charCode >= 0x41 && charCode <= 0x46) return charCode - 0x41 + 10;
    // a-f
    if (charCode >= 0x61 && charCode <= 0x66) return charCode - 0x61 + 10;
    return -1;
  }

  static bool _constantTimeEquals(List<int> a, List<int> b) {
    if (a.length != b.length) return false;
    var diff = 0;
    for (var i = 0; i < a.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }
}

class _ParsedHeader {
  _ParsedHeader(this.timestamp, this.v1);

  final int timestamp;
  final String v1;
}
