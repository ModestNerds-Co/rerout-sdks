//
//  rerout
//  rerout_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

// Mock response payloads use untyped literals for readability.
// ignore_for_file: inference_failure_on_collection_literal
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

  group('Rerout.initialize', () {
    test('throws ReroutException when apiKey is blank', () {
      expect(
        () => Rerout.initialize(apiKey: ''),
        throwsA(
          isA<ReroutException>().having((e) => e.code, 'code', 'missing_api_key'),
        ),
      );
      expect(
        () => Rerout.initialize(apiKey: '   '),
        throwsA(isA<ReroutException>()),
      );
    });

    test('trims trailing slashes from baseUrl', () {
      final r = Rerout.initialize(
        apiKey: 'rrk_test',
        baseUrl: 'https://api.example.com/',
      );
      expect(r.baseUrl, 'https://api.example.com');
      final r2 = Rerout.initialize(
        apiKey: 'rrk_test',
        baseUrl: 'https://api.example.com///',
      );
      expect(r2.baseUrl, 'https://api.example.com');
    });

    test('uses production baseUrl by default', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.baseUrl, kReroutDefaultBaseUrl);
    });

    test('exposes namespaces', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.links, isA<Links>());
      expect(r.project, isA<Project>());
      expect(r.qr, isA<Qr>());
    });
  });

  group('Links via mocked Dio', () {
    late _MockAdapter adapter;
    late Dio dio;
    late Rerout rerout;

    setUp(() {
      adapter = _MockAdapter();
      dio = Dio();
      dio.httpClientAdapter = adapter;
      rerout = Rerout.initialize(apiKey: 'rrk_test', dio: dio);
    });

    void respondWith({
      required int status,
      required Object body,
      Map<String, List<String>>? headers,
    }) {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          body is String ? body : jsonEncode(body),
          status,
          headers: headers ?? {
            'content-type': ['application/json'],
          },
        ),
      );
    }

    test('create returns Success<ShortLink> on 201', () async {
      respondWith(
        status: 201,
        body: {
          'code': 'q4',
          'short_url': 'https://rerout.co/q4',
          'target_url': 'https://example.com',
          'project_id': 'prj_test',
          'is_active': true,
          'seo_noindex': true,
          'created_at': 1700000000,
          'updated_at': 1700000000,
          'domain_hostname': null,
          'expires_at': null,
        },
      );
      final result = await rerout.links.create(
        const CreateLinkRequest(targetUrl: 'https://example.com'),
      );
      expect(result, isA<Success<ShortLink>>());
      expect(result.dataOrNull?.code, 'q4');
      expect(result.dataOrNull?.shortUrl, 'https://rerout.co/q4');
    });

    test('create surfaces server error code + message', () async {
      respondWith(
        status: 400,
        body: {
          'code': 'bad_target_url',
          'message': 'target_url must use https.',
        },
      );
      final result = await rerout.links.create(
        const CreateLinkRequest(targetUrl: 'http://insecure.example'),
      );
      expect(result, isA<Error<ShortLink>>());
      expect(result.errorOrNull?.code, 'bad_target_url');
      expect(result.errorOrNull?.statusCode, 400);
      expect(result.errorOrNull?.message, 'target_url must use https.');
    });

    test('synthesizes codes for status-only errors', () async {
      respondWith(status: 429, body: <String, dynamic>{});
      final result = await rerout.links.list();
      expect(result.errorOrNull?.code, 'rate_limited');
      expect(result.errorOrNull?.isRateLimited, isTrue);
    });

    test('reports network errors with code=network_error', () async {
      when(() => adapter.fetch(any(), any(), any())).thenThrow(
        DioException(
          requestOptions: RequestOptions(path: '/v1/links'),
          type: DioExceptionType.connectionError,
          message: 'socket hang up',
        ),
      );
      final result = await rerout.links.list();
      expect(result.errorOrNull?.code, 'network_error');
      expect(result.errorOrNull?.statusCode, 0);
    });

    test('reports timeouts with code=timeout', () async {
      when(() => adapter.fetch(any(), any(), any())).thenThrow(
        DioException(
          requestOptions: RequestOptions(path: '/v1/links'),
          type: DioExceptionType.connectionTimeout,
        ),
      );
      final result = await rerout.links.list();
      expect(result.errorOrNull?.code, 'timeout');
    });

    test('list passes cursor + limit as query params', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({'links': [], 'next_cursor': null}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });
      await rerout.links.list(cursor: 42, limit: 25);
      expect(captured?.queryParameters['cursor'], 42);
      expect(captured?.queryParameters['limit'], 25);
    });

    test('get encodes the code', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'code': 'go/promo',
            'short_url': 'https://go.brand.com/go/promo',
            'target_url': 'https://example.com',
            'project_id': 'prj_test',
            'is_active': true,
            'seo_noindex': true,
            'created_at': 1700000000,
            'updated_at': 1700000000,
          }),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });
      await rerout.links.get('go/promo');
      expect(captured?.path, 'https://api.rerout.co/v1/links/go%2Fpromo');
    });

    test('update with no fields returns Error without hitting the network', () async {
      final result = await rerout.links.update('abc', const UpdateLinkRequest());
      expect(result, isA<Error<ShortLink>>());
      verifyNever(() => adapter.fetch(any(), any(), any()));
    });

    test('update clearExpiresAt sends null', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'code': 'abc',
            'short_url': 'https://rerout.co/abc',
            'target_url': 'https://example.com',
            'project_id': 'prj_test',
            'is_active': true,
            'seo_noindex': true,
            'created_at': 1700000000,
            'updated_at': 1700000000,
          }),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });
      await rerout.links.update(
        'abc',
        const UpdateLinkRequest(clearExpiresAt: true),
      );
      final body = captured?.data;
      expect(body, isA<Map<String, dynamic>>());
      expect((body! as Map)['expires_at'], isNull);
      expect((body as Map).containsKey('expires_at'), isTrue);
    });

    test('delete returns Success<true> for 200', () async {
      respondWith(status: 200, body: {'deleted': true});
      final result = await rerout.links.delete('abc');
      expect(result, isA<Success<bool>>());
      expect(result.dataOrNull, isTrue);
    });

    test('stats defaults to days=30', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'code': 'abc',
            'days': 30,
            'total_clicks': 42,
            'qr_scans': 5,
            'countries': [],
            'referrers': [],
          }),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });
      final result = await rerout.links.stats('abc');
      expect(captured?.queryParameters['days'], 30);
      expect(result.dataOrNull?.totalClicks, 42);
      expect(result.dataOrNull?.qrScans, 5);
    });
  });
}
