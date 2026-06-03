//
//  rerout
//  webhooks_management_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
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

const Map<String, dynamic> _sampleWebhook = {
  'id': 'wh_abc123',
  'project_id': 'prj_test',
  'name': 'Order events',
  'url': 'https://example.com/hooks/rerout',
  'events': ['link.created', 'link.clicked'],
  'is_active': true,
  'payload_format': 'json',
  'created_at': 1700000000,
  'updated_at': 1700000000,
  'last_delivery_at': null,
  'last_success_at': null,
  'last_failure_at': null,
};

void main() {
  setUpAll(() {
    registerFallbackValue(RequestOptions(path: ''));
  });

  group('Rerout exposes webhooks namespace', () {
    test('webhooks namespace is present', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.webhooks, isA<Webhooks>());
    });
  });

  group('Webhooks via mocked Dio', () {
    late _MockAdapter adapter;
    late Dio dio;
    late Rerout rerout;

    setUp(() {
      adapter = _MockAdapter();
      dio = Dio();
      dio.httpClientAdapter = adapter;
      rerout = Rerout.initialize(apiKey: 'rrk_test', dio: dio);
    });

    test('create posts to /v1/projects/me/webhooks and returns the signing secret', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'endpoint': _sampleWebhook,
            'signing_secret': 'whsec_supersecret',
          }),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.webhooks.create(
        const CreateWebhookRequest(
          name: 'Order events',
          url: 'https://example.com/hooks/rerout',
          events: ['link.created', 'link.clicked'],
        ),
      );

      expect(captured?.method, 'POST');
      expect(captured?.path, 'https://api.rerout.co/v1/projects/me/webhooks');
      final body = captured?.data;
      expect(body, isA<Map<String, dynamic>>());
      expect(body, {
        'name': 'Order events',
        'url': 'https://example.com/hooks/rerout',
        'events': ['link.created', 'link.clicked'],
      });

      expect(result, isA<Success<CreatedWebhook>>());
      expect(result.dataOrNull?.signingSecret, 'whsec_supersecret');
      expect(result.dataOrNull?.endpoint.id, 'wh_abc123');
      expect(result.dataOrNull?.endpoint.events, ['link.created', 'link.clicked']);
    });

    test('list returns endpoints and supported event types', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'endpoints': [_sampleWebhook],
            'event_types': ['link.created', 'link.clicked', 'domain.verified'],
          }),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.webhooks.list();

      expect(captured?.method, 'GET');
      expect(captured?.path, 'https://api.rerout.co/v1/projects/me/webhooks');
      expect(result, isA<Success<ListWebhooksResult>>());
      expect(result.dataOrNull?.endpoints, hasLength(1));
      expect(result.dataOrNull?.endpoints.first.url, 'https://example.com/hooks/rerout');
      expect(result.dataOrNull?.eventTypes, contains('domain.verified'));
    });

    test('delete sends DELETE and encodes the endpoint id', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({'deleted': true}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.webhooks.delete('wh_abc123');

      expect(captured?.method, 'DELETE');
      expect(captured?.path, 'https://api.rerout.co/v1/projects/me/webhooks/wh_abc123');
      expect(result, isA<Success<bool>>());
      expect(result.dataOrNull, isTrue);
    });

    test('create forwards is_active and payload_format when provided', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((invocation) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'endpoint': {
              ..._sampleWebhook,
              'is_active': false,
              'payload_format': 'slack',
            },
            'signing_secret': 'whsec_x',
          }),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.webhooks.create(
        const CreateWebhookRequest(
          name: 'Slack',
          url: 'https://hooks.slack.com/services/T/B/x',
          events: ['link.created'],
          isActive: false,
          payloadFormat: 'slack',
        ),
      );

      final body = captured?.data as Map<String, dynamic>?;
      expect(body?['is_active'], false);
      expect(body?['payload_format'], 'slack');
      expect(result.dataOrNull?.endpoint.payloadFormat, 'slack');
      expect(result.dataOrNull?.endpoint.isActive, false);
    });

    test('create surfaces server error code + message', () async {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          jsonEncode({
            'code': 'invalid_webhook_url',
            'message': 'url must be a public https URL.',
          }),
          400,
          headers: {
            'content-type': ['application/json'],
          },
        ),
      );

      final result = await rerout.webhooks.create(
        const CreateWebhookRequest(
          name: 'Bad',
          url: 'http://insecure.example',
          events: ['link.created'],
        ),
      );

      expect(result, isA<Error<CreatedWebhook>>());
      expect(result.errorOrNull?.code, 'invalid_webhook_url');
      expect(result.errorOrNull?.statusCode, 400);
    });
  });
}
