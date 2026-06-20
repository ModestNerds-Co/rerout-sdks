//
//  rerout
//  tags_management_test.dart
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
import 'package:test/test.dart' hide Tags;

class _MockAdapter extends Mock implements HttpClientAdapter {}

const Map<String, dynamic> _sampleTag = {
  'id': 'tag_abc123',
  'name': 'Spring 2026',
  'color': 'teal',
};

const Map<String, dynamic> _sampleSummary = {
  'id': 'tag_abc123',
  'name': 'Spring 2026',
  'color': 'teal',
  'link_count': 4,
};

void main() {
  setUpAll(() {
    registerFallbackValue(RequestOptions(path: ''));
  });

  group('Rerout exposes tags namespace', () {
    test('tags namespace is present', () {
      final r = Rerout.initialize(apiKey: 'rrk_test');
      expect(r.tags, isA<Tags>());
    });
  });

  group('Tags via mocked Dio', () {
    late _MockAdapter adapter;
    late Dio dio;
    late Rerout rerout;

    setUp(() {
      adapter = _MockAdapter();
      dio = Dio();
      dio.httpClientAdapter = adapter;
      rerout = Rerout.initialize(apiKey: 'rrk_test', dio: dio);
    });

    test('list GETs /v1/projects/me/tags and returns link counts', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({
            'tags': [_sampleSummary],
          }),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.list();

      expect(captured?.method, 'GET');
      expect(captured?.path, 'https://api.rerout.co/v1/projects/me/tags');
      expect(captured?.data, isNull);
      expect(result, isA<Success<ListTagsResult>>());
      expect(result.dataOrNull?.tags, hasLength(1));
      expect(result.dataOrNull?.tags.first.id, 'tag_abc123');
      expect(result.dataOrNull?.tags.first.linkCount, 4);
    });

    test('create POSTs the name and color and returns the tag', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode(_sampleTag),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.create(
        const CreateTagRequest(name: 'Spring 2026', color: 'teal'),
      );

      expect(captured?.method, 'POST');
      expect(captured?.path, 'https://api.rerout.co/v1/projects/me/tags');
      expect(captured?.data, {'name': 'Spring 2026', 'color': 'teal'});
      expect(result, isA<Success<Tag>>());
      expect(result.dataOrNull?.id, 'tag_abc123');
    });

    test('create omits color when not provided', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode(_sampleTag),
          201,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.create(
        const CreateTagRequest(name: 'Spring 2026'),
      );

      expect(captured?.data, {'name': 'Spring 2026'});
      expect(result, isA<Success<Tag>>());
    });

    test('update PATCHes the tag by id and encodes it', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({..._sampleTag, 'color': 'red'}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.update(
        'tag_abc123',
        const UpdateTagRequest(color: 'red'),
      );

      expect(captured?.method, 'PATCH');
      expect(
        captured?.path,
        'https://api.rerout.co/v1/projects/me/tags/tag_abc123',
      );
      expect(captured?.data, {'color': 'red'});
      expect(result, isA<Success<Tag>>());
      expect(result.dataOrNull?.color, 'red');
    });

    test('update forwards only the provided fields', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({..._sampleTag, 'name': 'Renamed'}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.update(
        'tag_abc123',
        const UpdateTagRequest(name: 'Renamed'),
      );

      expect(captured?.data, {'name': 'Renamed'});
      expect(result.dataOrNull?.name, 'Renamed');
    });

    test(
      'update with an empty patch errors client-side without a request',
      () async {
        var fetched = false;
        when(() => adapter.fetch(any(), any(), any())).thenAnswer((_) async {
          fetched = true;
          return ResponseBody.fromString('{}', 200);
        });

        final result = await rerout.tags.update(
          'tag_abc123',
          const UpdateTagRequest(),
        );

        expect(fetched, isFalse);
        expect(result, isA<Error<Tag>>());
        expect(result.errorOrNull?.code, 'bad_request');
      },
    );

    test('update url-encodes a tag id with special characters', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode(_sampleTag),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      await rerout.tags.update(
        'go/promo',
        const UpdateTagRequest(name: 'X'),
      );

      expect(
        captured?.path,
        'https://api.rerout.co/v1/projects/me/tags/go%2Fpromo',
      );
    });

    test('delete sends DELETE and returns the result', () async {
      RequestOptions? captured;
      when(() => adapter.fetch(any(), any(), any())).thenAnswer((
        invocation,
      ) async {
        captured = invocation.positionalArguments[0] as RequestOptions;
        return ResponseBody.fromString(
          jsonEncode({'deleted': true}),
          200,
          headers: {
            'content-type': ['application/json'],
          },
        );
      });

      final result = await rerout.tags.delete('tag_abc123');

      expect(captured?.method, 'DELETE');
      expect(
        captured?.path,
        'https://api.rerout.co/v1/projects/me/tags/tag_abc123',
      );
      expect(result, isA<Success<bool>>());
      expect(result.dataOrNull, isTrue);
    });

    test('create surfaces server error code + message', () async {
      when(() => adapter.fetch(any(), any(), any())).thenAnswer(
        (_) async => ResponseBody.fromString(
          jsonEncode({
            'code': 'invalid_tag_color',
            'message': 'color must be one of the supported palette values.',
          }),
          400,
          headers: {
            'content-type': ['application/json'],
          },
        ),
      );

      final result = await rerout.tags.create(
        const CreateTagRequest(name: 'Spring', color: 'chartreuse'),
      );

      expect(result, isA<Error<Tag>>());
      expect(result.errorOrNull?.code, 'invalid_tag_color');
      expect(result.errorOrNull?.statusCode, 400);
    });
  });
}
