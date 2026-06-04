//
//  rerout
//  models_test.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

// Test fixtures intentionally use untyped literals for readability — the
// SDK code under test handles the dynamic shapes.
// ignore_for_file: inference_failure_on_collection_literal
// ignore_for_file: prefer_const_literals_to_create_immutables

import 'package:rerout/rerout.dart';
import 'package:test/test.dart';

void main() {
  group('ReroutException', () {
    test('isServerError + isRateLimited flags', () {
      const e1 = ReroutException(
        message: 'x',
        statusCode: 429,
        code: 'rate_limited',
      );
      expect(e1.isRateLimited, isTrue);
      expect(e1.isServerError, isFalse);

      const e2 = ReroutException(
        message: 'y',
        statusCode: 503,
        code: 'server_error',
      );
      expect(e2.isServerError, isTrue);
      expect(e2.isRateLimited, isFalse);

      const e3 = ReroutException(
        message: 'z',
        statusCode: 400,
        code: 'bad_request',
      );
      expect(e3.isServerError, isFalse);
      expect(e3.isRateLimited, isFalse);
    });
  });

  group('Result<T>', () {
    test('Success carries data and is detectable', () {
      const result = Success<int>(42);
      expect(result.isSuccess, isTrue);
      expect(result.isError, isFalse);
      expect(result.dataOrNull, 42);
      expect(result.errorOrNull, isNull);
    });

    test('Error carries the exception', () {
      const err = ReroutException(message: 'm', statusCode: 400, code: 'x');
      const result = Error<int>(err);
      expect(result.isError, isTrue);
      expect(result.isSuccess, isFalse);
      expect(result.dataOrNull, isNull);
      expect(result.errorOrNull, err);
    });

    test('map transforms data and passes through errors', () {
      const Result<int> ok = Success(10);
      final doubled = ok.map((n) => n * 2);
      expect(doubled.dataOrNull, 20);

      const err = ReroutException(message: 'm', statusCode: 0, code: 'x');
      const Result<int> bad = Error(err);
      final mapped = bad.map((n) => n * 2);
      expect(mapped.errorOrNull, err);
    });

    test('flatMap chains', () {
      const Result<int> ok = Success(10);
      final chained = ok.flatMap<int>((n) => Success(n + 1));
      expect(chained.dataOrNull, 11);
    });

    test('when executes the matching branch', () {
      const Result<int> ok = Success(7);
      final label = ok.when(
        success: (n) => 'got $n',
        error: (e) => 'err ${e.code}',
      );
      expect(label, 'got 7');
    });
  });

  group('CreateLinkRequest', () {
    test('omits null optional fields from JSON', () {
      const r = CreateLinkRequest(targetUrl: 'https://example.com');
      final json = r.toJson();
      expect(json.keys, ['target_url']);
    });

    test('includes every set field', () {
      const r = CreateLinkRequest(
        targetUrl: 'https://example.com',
        domainHostname: 'go.brand.com',
        code: 'q4',
        expiresAt: 1700000000,
        seoTitle: 't',
        seoDescription: 'd',
        seoImageUrl: 'https://img',
        seoCanonicalUrl: 'https://c',
        seoNoindex: false,
      );
      final json = r.toJson();
      expect(json['domain_hostname'], 'go.brand.com');
      expect(json['code'], 'q4');
      expect(json['seo_noindex'], false);
    });
  });

  group('CreateLinkRequest Smart Links', () {
    test('forwards password, max_clicks, track_conversions', () {
      const r = CreateLinkRequest(
        targetUrl: 'https://example.com',
        password: 's3cret',
        maxClicks: 100,
        trackConversions: true,
      );
      final json = r.toJson();
      expect(json['password'], 's3cret');
      expect(json['max_clicks'], 100);
      expect(json['track_conversions'], true);
    });

    test('serializes routing_rules and ab_variants', () {
      const r = CreateLinkRequest(
        targetUrl: 'https://example.com',
        routingRules: [
          RoutingRule(
            conditionType: 'country',
            conditionOp: 'is',
            conditionValue: 'ZA',
            targetUrl: 'https://example.com/za',
          ),
        ],
        abVariants: [
          AbVariantInput(targetUrl: 'https://example.com/a'),
          AbVariantInput(targetUrl: 'https://example.com/b', weight: 3),
        ],
      );
      final json = r.toJson();
      expect(json['routing_rules'], [
        {
          'condition_type': 'country',
          'condition_op': 'is',
          'condition_value': 'ZA',
          'target_url': 'https://example.com/za',
        },
      ]);
      expect(json['ab_variants'], [
        {'target_url': 'https://example.com/a'},
        {'target_url': 'https://example.com/b', 'weight': 3},
      ]);
    });
  });

  group('UpdateLinkRequest', () {
    test('isEmpty when nothing is set', () {
      const r = UpdateLinkRequest();
      expect(r.isEmpty, isTrue);
      expect(r.toJson(), <String, dynamic>{});
    });

    test('clearPassword and clearMaxClicks emit explicit nulls', () {
      const r = UpdateLinkRequest(clearPassword: true, clearMaxClicks: true);
      expect(r.toJson(), {'password': null, 'max_clicks': null});
      expect(r.isEmpty, isFalse);
    });

    test('clear flags override set values', () {
      const r = UpdateLinkRequest(
        password: 'x',
        clearPassword: true,
        maxClicks: 5,
        clearMaxClicks: true,
      );
      expect(r.toJson(), {'password': null, 'max_clicks': null});
    });

    test('full-replaces routing_rules and ab_variants', () {
      const r = UpdateLinkRequest(
        trackConversions: false,
        routingRules: [
          RoutingRule(
            conditionType: 'device',
            conditionOp: 'in',
            conditionValue: 'mobile,tablet',
            targetUrl: 'https://example.com/m',
          ),
        ],
        abVariants: [
          AbVariantInput(targetUrl: 'https://example.com/x', weight: 2),
        ],
      );
      final json = r.toJson();
      expect(json['track_conversions'], false);
      expect(json['routing_rules'], [
        {
          'condition_type': 'device',
          'condition_op': 'in',
          'condition_value': 'mobile,tablet',
          'target_url': 'https://example.com/m',
        },
      ]);
      expect(json['ab_variants'], [
        {'target_url': 'https://example.com/x', 'weight': 2},
      ]);
    });

    test('clearExpiresAt emits explicit null', () {
      const r = UpdateLinkRequest(clearExpiresAt: true);
      expect(r.toJson(), {'expires_at': null});
      expect(r.isEmpty, isFalse);
    });

    test('clearExpiresAt overrides expiresAt value', () {
      const r = UpdateLinkRequest(
        expiresAt: 1700000000,
        clearExpiresAt: true,
      );
      expect(r.toJson(), {'expires_at': null});
    });

    test('clear SEO flags emit explicit nulls', () {
      const r = UpdateLinkRequest(
        clearSeoTitle: true,
        clearSeoDescription: true,
        clearSeoImageUrl: true,
        clearSeoCanonicalUrl: true,
      );
      expect(r.toJson(), {
        'seo_title': null,
        'seo_description': null,
        'seo_image_url': null,
        'seo_canonical_url': null,
      });
    });
  });

  group('ShortLink.fromJson', () {
    test('parses all fields', () {
      final link = ShortLink.fromJson({
        'code': 'q4',
        'short_url': 'https://rerout.co/q4',
        'target_url': 'https://example.com',
        'project_id': 'prj_test',
        'is_active': true,
        'seo_noindex': false,
        'created_at': 1700000000,
        'updated_at': 1700000100,
        'domain_hostname': 'go.brand.com',
        'expires_at': 1701000000,
        'seo_title': 't',
        'seo_description': 'd',
        'seo_image_url': 'https://img',
        'seo_canonical_url': 'https://c',
        'seo_updated_at': 1700000050,
        'tags': [
          {'id': 'tag_1', 'name': 'Launch', 'color': '#FF8800'},
          {'id': 'tag_2', 'name': 'Sale', 'color': '#00AAFF'},
        ],
      });
      expect(link.code, 'q4');
      expect(link.shortUrl, 'https://rerout.co/q4');
      expect(link.targetUrl, 'https://example.com');
      expect(link.domainHostname, 'go.brand.com');
      expect(link.expiresAt, 1701000000);
      expect(link.isActive, isTrue);
      expect(link.seoNoindex, isFalse);
      expect(link.seoTitle, 't');
      expect(link.tags.length, 2);
      expect(link.tags.first.id, 'tag_1');
      expect(link.tags.first.name, 'Launch');
      expect(link.tags.first.color, '#FF8800');
    });

    test('defaults tags to an empty list when absent', () {
      final link = ShortLink.fromJson({
        'code': 'q4',
        'short_url': 'https://rerout.co/q4',
        'target_url': 'https://example.com',
        'project_id': 'prj_test',
        'is_active': true,
        'seo_noindex': true,
        'created_at': 1700000000,
        'updated_at': 1700000100,
      });
      expect(link.tags, isEmpty);
    });

    test('parses Smart Links fields', () {
      final link = ShortLink.fromJson({
        'code': 'q4',
        'short_url': 'https://rerout.co/q4',
        'target_url': 'https://example.com',
        'project_id': 'prj_test',
        'is_active': true,
        'seo_noindex': true,
        'created_at': 1700000000,
        'updated_at': 1700000100,
        'password_protected': true,
        'max_clicks': 100,
        'click_count': 42,
        'track_conversions': true,
        'routing_rules': [
          {
            'condition_type': 'country',
            'condition_op': 'is',
            'condition_value': 'ZA',
            'target_url': 'https://example.com/za',
          },
        ],
        'ab_variants': [
          {'id': 1, 'target_url': 'https://example.com/a', 'weight': 1},
          {'id': 2, 'target_url': 'https://example.com/b', 'weight': 3},
        ],
      });
      expect(link.passwordProtected, isTrue);
      expect(link.maxClicks, 100);
      expect(link.clickCount, 42);
      expect(link.trackConversions, isTrue);
      expect(link.routingRules, hasLength(1));
      expect(link.routingRules.first.conditionValue, 'ZA');
      expect(link.abVariants, hasLength(2));
      expect(link.abVariants[1].id, 2);
      expect(link.abVariants[1].weight, 3);
    });

    test('defaults Smart Links fields when absent', () {
      final link = ShortLink.fromJson({
        'code': 'q4',
        'short_url': 'https://rerout.co/q4',
        'target_url': 'https://example.com',
        'project_id': 'prj_test',
        'is_active': true,
        'seo_noindex': true,
        'created_at': 1700000000,
        'updated_at': 1700000100,
      });
      expect(link.passwordProtected, isFalse);
      expect(link.maxClicks, isNull);
      expect(link.clickCount, 0);
      expect(link.trackConversions, isFalse);
      expect(link.routingRules, isEmpty);
      expect(link.abVariants, isEmpty);
    });
  });

  group('RecordConversionRequest', () {
    test('omits null optional fields', () {
      const r = RecordConversionRequest(
        clickId: 'rrid_abc',
        eventName: 'purchase',
      );
      expect(r.toJson(), {'click_id': 'rrid_abc', 'event_name': 'purchase'});
    });

    test('includes value_cents and currency when set', () {
      const r = RecordConversionRequest(
        clickId: 'rrid_abc',
        eventName: 'purchase',
        valueCents: 4999,
        currency: 'USD',
      );
      final json = r.toJson();
      expect(json['value_cents'], 4999);
      expect(json['currency'], 'USD');
    });
  });

  group('RecordedConversion.fromJson', () {
    test('parses recorded + duplicate', () {
      final r = RecordedConversion.fromJson({
        'recorded': true,
        'duplicate': true,
      });
      expect(r.recorded, isTrue);
      expect(r.duplicate, isTrue);
    });

    test('defaults to false when fields absent', () {
      final r = RecordedConversion.fromJson(<String, dynamic>{});
      expect(r.recorded, isFalse);
      expect(r.duplicate, isFalse);
    });
  });

  group('BatchCreateLinksResult.fromJson', () {
    test('parses created, total, and per-item results', () {
      final r = BatchCreateLinksResult.fromJson({
        'created': 1,
        'total': 2,
        'results': [
          {'index': 0, 'ok': true, 'code': 'aaa111'},
          {'index': 1, 'ok': false, 'error': 'code already in use'},
        ],
      });
      expect(r.created, 1);
      expect(r.total, 2);
      expect(r.results, hasLength(2));
      expect(r.results.first.code, 'aaa111');
      expect(r.results[1].ok, isFalse);
      expect(r.results[1].error, 'code already in use');
    });
  });

  group('BatchLinkInput.toJson', () {
    test('omits null optional fields', () {
      const item = BatchLinkInput(targetUrl: 'https://example.com');
      expect(item.toJson(), {'target_url': 'https://example.com'});
    });

    test('includes code, expires_at, domain_hostname when set', () {
      const item = BatchLinkInput(
        targetUrl: 'https://example.com',
        code: 'two',
        expiresAt: 1700000000,
        domainHostname: 'go.brand.com',
      );
      expect(item.toJson(), {
        'target_url': 'https://example.com',
        'code': 'two',
        'expires_at': 1700000000,
        'domain_hostname': 'go.brand.com',
      });
    });
  });

  group('ListLinksResult.fromJson', () {
    test('hasMore is true when next_cursor is non-null', () {
      final r = ListLinksResult.fromJson({'links': [], 'next_cursor': 42});
      expect(r.nextCursor, 42);
      expect(r.hasMore, isTrue);
    });

    test('hasMore is false when next_cursor is null', () {
      final r = ListLinksResult.fromJson({'links': [], 'next_cursor': null});
      expect(r.hasMore, isFalse);
    });
  });

  group('ProjectStats.fromJson', () {
    test('parses arrays and totals', () {
      final stats = ProjectStats.fromJson({
        'days': 7,
        'total_clicks': 100,
        'qr_scans': 25,
        'daily': [
          {'day': 1700000000, 'clicks': 10, 'qr_scans': 2},
          {'day': 1700086400, 'clicks': 20, 'qr_scans': 5},
        ],
        'countries': [
          {'value': 'ZA', 'clicks': 60},
          {'value': 'US', 'clicks': 40},
        ],
        'referrers': [],
        'devices': [
          {'value': 'mobile', 'clicks': 80},
        ],
        'browsers': [],
        'top_codes': [
          {'value': 'q4', 'clicks': 100},
        ],
      });
      expect(stats.days, 7);
      expect(stats.totalClicks, 100);
      expect(stats.qrScans, 25);
      expect(stats.daily.length, 2);
      expect(stats.daily.first.clicks, 10);
      expect(stats.countries.first.value, 'ZA');
      expect(stats.topCodes.first.clicks, 100);
    });

    test('handles missing arrays gracefully', () {
      final stats = ProjectStats.fromJson({
        'days': 30,
        'total_clicks': 0,
        'qr_scans': 0,
      });
      expect(stats.daily, isEmpty);
      expect(stats.countries, isEmpty);
    });
  });

  group('QrOptions.toQueryParameters', () {
    test('emits only set fields', () {
      expect(const QrOptions().toQueryParameters(), isEmpty);
      final params = const QrOptions(size: 10, ecc: 'Q').toQueryParameters();
      expect(params, {'size': '10', 'ecc': 'Q'});
    });

    test('refresh=true maps to "1"', () {
      expect(
        const QrOptions(refresh: true).toQueryParameters()['refresh'],
        '1',
      );
    });

    test('refresh=string is passed through', () {
      expect(
        const QrOptions(refresh: 'v3').toQueryParameters()['refresh'],
        'v3',
      );
    });
  });
}
