//
//  rerout
//  rerout.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:dio/dio.dart';
import 'package:rerout/src/models/batch_create_links.dart';
import 'package:rerout/src/models/create_link_request.dart';
import 'package:rerout/src/models/create_webhook_request.dart';
import 'package:rerout/src/models/created_webhook.dart';
import 'package:rerout/src/models/link_stats.dart';
import 'package:rerout/src/models/list_links_result.dart';
import 'package:rerout/src/models/list_webhooks_result.dart';
import 'package:rerout/src/models/project_stats.dart';
import 'package:rerout/src/models/qr_options.dart';
import 'package:rerout/src/models/record_conversion_request.dart';
import 'package:rerout/src/models/recorded_conversion.dart';
import 'package:rerout/src/models/rerout_exception.dart';
import 'package:rerout/src/models/result.dart';
import 'package:rerout/src/models/short_link.dart';
import 'package:rerout/src/models/update_link_request.dart';

/// Default production API URL.
const String kReroutDefaultBaseUrl = 'https://api.rerout.co';

/// Official Dart SDK for the Rerout branded-link API.
///
/// ## Usage
///
/// ```dart
/// import 'package:rerout/rerout.dart';
///
/// final rerout = Rerout.initialize(apiKey: 'rrk_...');
///
/// final result = await rerout.links.create(
///   const CreateLinkRequest(
///     targetUrl: 'https://example.com/q4-sale',
///     domainHostname: 'go.brand.com',
///     code: 'q4',
///   ),
/// );
///
/// switch (result) {
///   case Success(:final data):
///     print('Short URL: ${data.shortUrl}');
///   case Error(:final error):
///     print('Failed: ${error.message}');
/// }
/// ```
class Rerout {
  Rerout._({
    required String apiKey,
    required String baseUrl,
    required Dio dio,
  }) : _apiKey = apiKey,
       _baseUrl = baseUrl,
       _dio = dio {
    links = Links._(this);
    project = Project._(this);
    qr = Qr._(this);
    webhooks = Webhooks._(this);
    conversions = Conversions._(this);
  }

  /// Creates a new instance of the Rerout SDK.
  ///
  /// ## Parameters
  ///
  /// - [apiKey]: Project API key from the dashboard (`rrk_…`). Required.
  /// - [baseUrl]: Override the API base URL. Defaults to
  ///   `https://api.rerout.co`. Useful for staging or self-hosted setups.
  /// - [dio]: Inject a pre-configured Dio instance. Useful for tests, custom
  ///   interceptors, proxies, etc.
  ///
  /// ## Example
  ///
  /// ```dart
  /// final rerout = Rerout.initialize(apiKey: 'rrk_...');
  /// ```
  factory Rerout.initialize({
    required String apiKey,
    String baseUrl = kReroutDefaultBaseUrl,
    Dio? dio,
  }) {
    if (apiKey.trim().isEmpty) {
      throw const ReroutException(
        code: 'missing_api_key',
        message: 'A project API key is required to construct Rerout.',
        statusCode: 0,
      );
    }
    return Rerout._(
      apiKey: apiKey,
      baseUrl: baseUrl.replaceAll(RegExp(r'/+$'), ''),
      dio: dio ?? Dio(),
    );
  }

  final String _apiKey;
  final String _baseUrl;
  final Dio _dio;

  /// Link operations: create, list, get, update, delete, stats.
  late final Links links;

  /// Project-level operations: aggregate stats, current project.
  late final Project project;

  /// QR helpers — URL builders and signed-fetch.
  late final Qr qr;

  /// Webhook endpoint management: create, list, delete.
  late final Webhooks webhooks;

  /// Conversion tracking: record a conversion against a prior click.
  late final Conversions conversions;

  /// The resolved API base URL. Exposed for diagnostics and the QR helper.
  String get baseUrl => _baseUrl;

  /// @internal — used by [Links], [Project], [Qr].
  Future<Result<T>> request<T>({
    required String method,
    required String path,
    required T Function(dynamic data) parse,
    Map<String, dynamic>? body,
    Map<String, dynamic>? query,
    ResponseType responseType = ResponseType.json,
  }) async {
    final url = '$_baseUrl$path';
    try {
      final response = await _dio.request<dynamic>(
        url,
        data: body,
        queryParameters: query,
        options: Options(
          method: method,
          responseType: responseType,
          headers: <String, String>{
            'authorization': 'Bearer $_apiKey',
            'accept': responseType == ResponseType.plain
                ? 'image/svg+xml,text/html'
                : 'application/json',
            if (body != null) 'content-type': 'application/json',
          },
          validateStatus: (status) => status != null && status < 600,
        ),
      );
      final status = response.statusCode ?? 0;
      if (status >= 200 && status < 300) {
        return Result.success(parse(response.data));
      }
      return Result.error(_errorFromResponse(response, path));
    } on DioException catch (e) {
      return Result.error(_errorFromDio(e, path));
    } on Exception catch (e) {
      return Result.error(
        ReroutException(
          code: 'client_error',
          message: e.toString(),
          statusCode: 0,
          path: path,
        ),
      );
    }
  }

  ReroutException _errorFromResponse(Response<dynamic> response, String path) {
    final status = response.statusCode ?? 0;
    final data = response.data;
    String? code;
    String? message;
    String? timestamp;
    if (data is Map<String, dynamic>) {
      code = data['code'] as String?;
      message = data['message'] as String?;
      timestamp = data['timestamp'] as String?;
    }
    return ReroutException(
      code: code ?? _synthCodeForStatus(status),
      message: message ?? 'Rerout returned HTTP $status.',
      statusCode: status,
      path: path,
      timestamp: timestamp,
    );
  }

  ReroutException _errorFromDio(DioException e, String path) {
    final response = e.response;
    if (response != null) {
      return _errorFromResponse(response, path);
    }
    final isTimeout =
        e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout;
    return ReroutException(
      code: isTimeout ? 'timeout' : 'network_error',
      message: e.message ?? 'Request to Rerout failed.',
      statusCode: 0,
      path: path,
    );
  }

  static String _synthCodeForStatus(int status) {
    if (status == 401) return 'unauthorized';
    if (status == 403) return 'forbidden';
    if (status == 404) return 'not_found';
    if (status == 429) return 'rate_limited';
    if (status >= 500 && status < 600) return 'server_error';
    return 'client_error';
  }
}

// ─── Namespaces ─────────────────────────────────────────────────────────────

/// Link operations namespace. Reached via [Rerout.links].
class Links {
  Links._(this._client);

  final Rerout _client;

  /// Creates a new short link.
  Future<Result<ShortLink>> create(CreateLinkRequest request) {
    return _client.request<ShortLink>(
      method: 'POST',
      path: '/v1/links',
      body: request.toJson(),
      parse: (data) => ShortLink.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Bulk-creates links in one request. Each item succeeds or fails
  /// independently; inspect `results[i].ok` for per-item outcomes.
  Future<Result<BatchCreateLinksResult>> createBatch(
    List<BatchLinkInput> links,
  ) {
    return _client.request<BatchCreateLinksResult>(
      method: 'POST',
      path: '/v1/links/batch',
      body: {'links': links.map((l) => l.toJson()).toList()},
      parse: (data) =>
          BatchCreateLinksResult.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Paginated list of links in the project.
  Future<Result<ListLinksResult>> list({int? cursor, int? limit}) {
    final query = <String, dynamic>{};
    if (cursor != null) query['cursor'] = cursor;
    if (limit != null) query['limit'] = limit;
    return _client.request<ListLinksResult>(
      method: 'GET',
      path: '/v1/links',
      query: query.isEmpty ? null : query,
      parse: (data) => ListLinksResult.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Get a single link by code.
  Future<Result<ShortLink>> get(String code) {
    return _client.request<ShortLink>(
      method: 'GET',
      path: '/v1/links/${Uri.encodeComponent(code)}',
      parse: (data) => ShortLink.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Patch a link. Only fields set on [request] are sent.
  Future<Result<ShortLink>> update(String code, UpdateLinkRequest request) {
    if (request.isEmpty) {
      return Future.value(
        Result.error(
          ReroutException(
            code: 'bad_request',
            message: 'UpdateLinkRequest has no fields to send.',
            statusCode: 0,
            path: '/v1/links/$code',
          ),
        ),
      );
    }
    return _client.request<ShortLink>(
      method: 'PATCH',
      path: '/v1/links/${Uri.encodeComponent(code)}',
      body: request.toJson(),
      parse: (data) => ShortLink.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Soft-deletes a link. The short URL stops redirecting and is gone from
  /// lists.
  Future<Result<bool>> delete(String code) {
    return _client.request<bool>(
      method: 'DELETE',
      path: '/v1/links/${Uri.encodeComponent(code)}',
      parse: (data) {
        if (data is Map<String, dynamic>) {
          return data['deleted'] as bool? ?? true;
        }
        return true;
      },
    );
  }

  /// Per-link click stats. Defaults to 30 days.
  Future<Result<LinkStats>> stats(String code, {int days = 30}) {
    return _client.request<LinkStats>(
      method: 'GET',
      path: '/v1/links/${Uri.encodeComponent(code)}/stats',
      query: {'days': days},
      parse: (data) => LinkStats.fromJson(data as Map<String, dynamic>),
    );
  }
}

/// Project-level operations namespace. Reached via [Rerout.project].
class Project {
  Project._(this._client);

  final Rerout _client;

  /// Aggregate stats across every link in the project.
  Future<Result<ProjectStats>> stats({int days = 30}) {
    return _client.request<ProjectStats>(
      method: 'GET',
      path: '/v1/projects/me/stats',
      query: {'days': days},
      parse: (data) => ProjectStats.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Returns info about the project that owns the current API key.
  Future<Result<Map<String, dynamic>>> me() {
    return _client.request<Map<String, dynamic>>(
      method: 'GET',
      path: '/v1/projects/me',
      parse: (data) =>
          (data as Map<String, dynamic>?) ?? const <String, dynamic>{},
    );
  }
}

/// Webhook endpoint management namespace. Reached via [Rerout.webhooks].
///
/// Manages the project's *outbound* webhook endpoints. To verify *inbound*
/// deliveries, use `ReroutWebhookSignature` instead.
class Webhooks {
  Webhooks._(this._client);

  final Rerout _client;

  /// Creates a webhook endpoint for the project that owns the API key.
  ///
  /// The returned [CreatedWebhook.signingSecret] is shown once — persist it to
  /// verify inbound deliveries.
  Future<Result<CreatedWebhook>> create(CreateWebhookRequest request) {
    return _client.request<CreatedWebhook>(
      method: 'POST',
      path: '/v1/projects/me/webhooks',
      body: request.toJson(),
      parse: (data) =>
          CreatedWebhook.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Lists webhook endpoints and the event types the server can deliver.
  Future<Result<ListWebhooksResult>> list() {
    return _client.request<ListWebhooksResult>(
      method: 'GET',
      path: '/v1/projects/me/webhooks',
      parse: (data) =>
          ListWebhooksResult.fromJson(data as Map<String, dynamic>),
    );
  }

  /// Soft-deletes an endpoint and abandons its pending deliveries.
  /// Idempotent.
  Future<Result<bool>> delete(String endpointId) {
    return _client.request<bool>(
      method: 'DELETE',
      path: '/v1/projects/me/webhooks/${Uri.encodeComponent(endpointId)}',
      parse: (data) {
        if (data is Map<String, dynamic>) {
          return data['deleted'] as bool? ?? true;
        }
        return true;
      },
    );
  }
}

/// Conversion tracking namespace. Reached via [Rerout.conversions].
class Conversions {
  Conversions._(this._client);

  final Rerout _client;

  /// Records a conversion attributed to a prior click via its `clickId`
  /// (`rrid`). Idempotent per `(clickId, eventName)`.
  Future<Result<RecordedConversion>> record(RecordConversionRequest request) {
    return _client.request<RecordedConversion>(
      method: 'POST',
      path: '/v1/conversions',
      body: request.toJson(),
      parse: (data) =>
          RecordedConversion.fromJson(data as Map<String, dynamic>),
    );
  }
}

/// QR helpers — URL builders and signed-fetch. Reached via [Rerout.qr].
class Qr {
  Qr._(this._client);

  final Rerout _client;

  /// Builds the URL the Rerout API serves the QR SVG from. Pure — does not
  /// call the API.
  ///
  /// ```dart
  /// final url = rerout.qr.url(
  ///   'q4',
  ///   options: const QrOptions(size: 12, ecc: 'H'),
  /// );
  /// ```
  String url(String code, {QrOptions? options}) {
    final base = _client.baseUrl.replaceAll(RegExp(r'/+$'), '');
    final uri = Uri.parse('$base/v1/links/${Uri.encodeComponent(code)}/qr');
    final params = options?.toQueryParameters() ?? const <String, String>{};
    if (params.isEmpty) return uri.toString();
    return uri.replace(queryParameters: params).toString();
  }

  /// Fetches the QR SVG body. Hits the same endpoint as [url] but attaches
  /// the bearer token and returns the rendered text.
  Future<Result<String>> svg(String code, {QrOptions? options}) {
    return _client.request<String>(
      method: 'GET',
      path: '/v1/links/${Uri.encodeComponent(code)}/qr',
      query: options?.toQueryParameters(),
      responseType: ResponseType.plain,
      parse: (data) => data is String ? data : data.toString(),
    );
  }
}
