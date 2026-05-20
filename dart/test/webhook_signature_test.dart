//
//  rerout
//  webhook_signature_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'dart:convert';

import 'package:crypto/crypto.dart';
import 'package:rerout/rerout.dart';
import 'package:test/test.dart';

const _secret = 'whsec_super_secret_value';
const _rawBody = '{"id":"evt_abc","type":"link.clicked","data":{"code":"q4"}}';

String _signedHeader({required int ts, String? secret, String? body}) {
  final s = secret ?? _secret;
  final b = body ?? _rawBody;
  final hmac = Hmac(sha256, utf8.encode(s))
      .convert(utf8.encode('$ts.$b'))
      .toString();
  return 't=$ts,v1=$hmac';
}

DateTime Function() _fixedClock(int unixSeconds) {
  return () => DateTime.fromMillisecondsSinceEpoch(unixSeconds * 1000);
}

void main() {
  group('ReroutWebhookSignature.verify', () {
    test('accepts a freshly signed payload', () {
      const ts = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: ts),
          secret: _secret,
          clock: _fixedClock(ts),
        ),
        isTrue,
      );
    });

    test('rejects when the HMAC is wrong (different secret)', () {
      const ts = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: ts, secret: 'whsec_other'),
          secret: _secret,
          clock: _fixedClock(ts),
        ),
        isFalse,
      );
    });

    test('rejects when the body has been tampered with', () {
      const ts = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: '$_rawBody ', // extra trailing space
          signatureHeader: _signedHeader(ts: ts),
          secret: _secret,
          clock: _fixedClock(ts),
        ),
        isFalse,
      );
    });

    test('rejects payloads outside the tolerance window', () {
      const signed = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: signed),
          secret: _secret,
          clock: _fixedClock(
            signed + ReroutWebhookSignature.defaultToleranceSeconds + 1,
          ),
        ),
        isFalse,
      );
    });

    test('accepts payloads exactly at the tolerance boundary', () {
      const signed = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: signed),
          secret: _secret,
          clock: _fixedClock(
            signed + ReroutWebhookSignature.defaultToleranceSeconds,
          ),
        ),
        isTrue,
      );
    });

    test('disables the timestamp check when toleranceSeconds=0', () {
      const signed = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: signed),
          secret: _secret,
          toleranceSeconds: 0,
          clock: _fixedClock(signed + 10000000),
        ),
        isTrue,
      );
    });

    test('rejects malformed headers', () {
      const ts = 1700000000;
      const cases = [
        '',
        'garbage',
        't=,v1=abc',
        'v1=abc',
        't=$ts',
        't=notanumber,v1=abc',
        't=-1,v1=abc',
        't=$ts,v1=nothex',
        't=$ts,v1=12345',
      ];
      for (final header in cases) {
        expect(
          ReroutWebhookSignature.verify(
            rawBody: _rawBody,
            signatureHeader: header,
            secret: _secret,
            clock: _fixedClock(ts),
          ),
          isFalse,
          reason: 'should reject "$header"',
        );
      }
    });

    test('handles header key casing variations', () {
      const ts = 1700000000;
      final hmac = Hmac(sha256, utf8.encode(_secret))
          .convert(utf8.encode('$ts.$_rawBody'))
          .toString();
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: 'T=$ts, V1=$hmac',
          secret: _secret,
          clock: _fixedClock(ts),
        ),
        isTrue,
      );
    });

    test('rejects when secret is empty', () {
      const ts = 1700000000;
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: _signedHeader(ts: ts),
          secret: '',
          clock: _fixedClock(ts),
        ),
        isFalse,
      );
    });

    test('rejects when signatureHeader is empty', () {
      expect(
        ReroutWebhookSignature.verify(
          rawBody: _rawBody,
          signatureHeader: '',
          secret: _secret,
          clock: _fixedClock(1700000000),
        ),
        isFalse,
      );
    });
  });
}
