//
//  rerout
//  qr_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:rerout/rerout.dart';
import 'package:test/test.dart';

void main() {
  group('Rerout.qr.url', () {
    test('returns the basic path with no options', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.qr.url('q4'), 'https://api.rerout.co/v1/links/q4/qr');
    });

    test('appends every option that is set', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      final url = Uri.parse(
        r.qr.url(
          'q4',
          options: const QrOptions(
            size: 12,
            margin: 2,
            ecc: 'H',
            domain: 'go.brand.com',
            refresh: true,
          ),
        ),
      );
      expect(url.path, '/v1/links/q4/qr');
      expect(url.queryParameters['size'], '12');
      expect(url.queryParameters['margin'], '2');
      expect(url.queryParameters['ecc'], 'H');
      expect(url.queryParameters['domain'], 'go.brand.com');
      expect(url.queryParameters['refresh'], '1');
    });

    test('omits unset options', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(
        r.qr.url('q4', options: const QrOptions(size: 8)),
        'https://api.rerout.co/v1/links/q4/qr?size=8',
      );
    });

    test('encodes special code characters', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(
        r.qr.url('hello world'),
        'https://api.rerout.co/v1/links/hello%20world/qr',
      );
    });

    test('honours a custom baseUrl', () {
      final r = Rerout.initialize(
        apiKey: 'rrk_test',
        baseUrl: 'https://api.staging.example.com',
      );
      expect(
        r.qr.url('q4'),
        'https://api.staging.example.com/v1/links/q4/qr',
      );
    });

    test('accepts a string refresh override', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      final url = Uri.parse(
        r.qr.url('q4', options: const QrOptions(refresh: 'v2')),
      );
      expect(url.queryParameters['refresh'], 'v2');
    });
  });
}
