//
//  rerout
//  smart_links_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

// Mock response payloads use untyped literals for readability.
// ignore_for_file: lines_longer_than_80_chars
// ignore_for_file: avoid_redundant_argument_values
// ignore_for_file: cascade_invocations

import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:mocktail/mocktail.dart';
import 'package:rerout/rerout.dart';
import 'package:test/test.dart';

class _MockAdapter extends Mock implements HttpClientAdapter {}

void main() {
  setUpAll(() {
    registerFallbackValue(RequestOptions(path: ''));
  });

  group('Rerout exposes conversions namespace', () {
    test('conversions namespace is present', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.conversions, isA<Conversions>());
    });
  });

  group('Links.createBatch via mocked Dio', () {
    late _MockAdapter adapter;
    late Dio dio;
    late Rerout rerout;

    setUp(() {
      adapter = _MockAdapter();
      dio = Dio();
      dio.httpClientAdapter = adapter;
      rerout = Rerout.initialize(apiKey: 'rrk_test', dio: dio);
    });

    test('posts to /v1/links/batch and returns per-item results', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'created': 1,
            'total': 2,
            'results': [
              {'index': 0, 'ok': true, 'code': 'aaa111'},
              {'index': 1, 'ok': false, 'error': 'code already in use'},
            ],
          }),
          207,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.links.createBatch(const [
        BatchLinkInput(targetUrl: 'https://example.com/1'),
        BatchLinkInput(targetUrl: 'https://example.com/2', code: 'two'),
      ]);

      expect(captured?.method, 'POST');
      expect(captured?.path, 'https://api.rerout.co/v1/links/batch');
      expect(captured?.data, {
        'links': [
          {'target_url': 'https://example.com/1'},
          {'target_url': 'https://example.com/2', 'code': 'two'},
        ],
      });

      expect(result, isA<Success<BatchCreateLinksResult>>());
      expect(result.dataOrNull?.created, 1);
      expect(result.dataOrNull?.total, 2);
      expect(result.dataOrNull?.results.first.code, 'aaa111');
      expect(result.dataOrNull?.results[1].ok, isFalse);
      expect(result.dataOrNull?.results[1].error, 'code already in use');
    });

    test('surfaces server error code + message', () async {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          jsonEncode({
            'code': 'bad_request',
            'message': 'links must be a non-empty array.',
          }),
          400,
          headers: {
            'content-type': ['application/json'],
          },
        ),
      );

      final result = await rerout.links.createBatch(const [
        BatchLinkInput(targetUrl: 'https://example.com/1'),
      ]);

      expect(result, isA<Error<BatchCreateLinksResult>>());
      expect(result.errorOrNull?.code, 'bad_request');
      expect(result.errorOrNull?.statusCode, 400);
    });
  });

  group('Conversions.record via mocked Dio', () {
    late _MockAdapter adapter;
    late Dio dio;
    late Rerout rerout;

    setUp(() {
      adapter = _MockAdapter();
      dio = Dio();
      dio.httpClientAdapter = adapter;
      rerout = Rerout.initialize(apiKey: 'rrk_test', dio: dio);
    });

    test('posts to /v1/conversions with click_id + event_name', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({'recorded': true, 'duplicate': false}),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.conversions.record(
        const RecordConversionRequest(
          clickId: 'rrid_abc',
          eventName: 'purchase',
        ),
      );

      expect(captured?.method, 'POST');
      expect(captured?.path, 'https://api.rerout.co/v1/conversions');
      expect(captured?.data, {'click_id': 'rrid_abc', 'event_name': 'purchase'});
      expect(result, isA<Success<RecordedConversion>>());
      expect(result.dataOrNull?.recorded, isTrue);
      expect(result.dataOrNull?.duplicate, isFalse);
    });

    test('forwards value_cents and currency when provided', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({'recorded': true, 'duplicate': false}),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      await rerout.conversions.record(
        const RecordConversionRequest(
          clickId: 'rrid_abc',
          eventName: 'purchase',
          valueCents: 4999,
          currency: 'USD',
        ),
      );

      final body = captured?.data as Map<String, dynamic>?;
      expect(body?['value_cents'], 4999);
      expect(body?['currency'], 'USD');
    });

    test('surfaces duplicate=true on a repeat conversion', () async {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          jsonEncode({'recorded': true, 'duplicate': true}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        ),
      );

      final result = await rerout.conversions.record(
        const RecordConversionRequest(
          clickId: 'rrid_abc',
          eventName: 'purchase',
        ),
      );

      expect(result.dataOrNull?.duplicate, isTrue);
    });

    test('surfaces server error code + message', () async {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          jsonEncode({
            'code': 'bad_request',
            'message': 'click_id is required.',
          }),
          400,
          headers: {
            'content-type': ['application/json'],
          },
        ),
      );

      final result = await rerout.conversions.record(
        const RecordConversionRequest(clickId: '', eventName: 'purchase'),
      );

      expect(result, isA<Error<RecordedConversion>>());
      expect(result.errorOrNull?.code, 'bad_request');
      expect(result.errorOrNull?.statusCode, 400);
    });
  });
}
