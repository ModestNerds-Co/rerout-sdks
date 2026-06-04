//
//  rerout
//  batch_create_links.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 04/06/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A single link to create in a batch.
@immutable
class BatchLinkInput {
  /// Creates a [BatchLinkInput].
  const BatchLinkInput({
    required this.targetUrl,
    this.code,
    this.expiresAt,
    this.domainHostname,
  });

  /// Absolute `https://` destination URL.
  final String targetUrl;

  /// Custom path. Only valid when [domainHostname] is provided.
  final String? code;

  /// Unix seconds — expiration. Omit for a permanent link.
  final int? expiresAt;

  /// Verified custom domain to host this link on.
  final String? domainHostname;

  /// Converts this item to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'target_url': targetUrl,
    if (code != null) 'code': code,
    if (expiresAt != null) 'expires_at': expiresAt,
    if (domainHostname != null) 'domain_hostname': domainHostname,
  };
}

/// Per-item outcome of a batch create.
@immutable
class BatchLinkResult {
  /// Creates a [BatchLinkResult].
  const BatchLinkResult({
    required this.index,
    required this.ok,
    this.code,
    this.error,
  });

  /// Parses a [BatchLinkResult] from the API JSON envelope.
  factory BatchLinkResult.fromJson(Map<String, dynamic> json) =>
      BatchLinkResult(
        index: json['index'] as int,
        ok: json['ok'] as bool? ?? false,
        code: json['code'] as String?,
        error: json['error'] as String?,
      );

  /// Index of the input item this result corresponds to.
  final int index;

  /// Whether the item was created.
  final bool ok;

  /// Allocated code, when [ok].
  final String? code;

  /// Failure reason, when not [ok].
  final String? error;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is BatchLinkResult &&
          index == other.index &&
          ok == other.ok &&
          code == other.code &&
          error == other.error;

  @override
  int get hashCode => Object.hash(index, ok, code, error);

  @override
  String toString() => 'BatchLinkResult(index: $index, ok: $ok, code: $code)';
}

/// Result of a batch link create (partial-success).
@immutable
class BatchCreateLinksResult {
  /// Creates a [BatchCreateLinksResult].
  const BatchCreateLinksResult({
    required this.created,
    required this.total,
    required this.results,
  });

  /// Parses a [BatchCreateLinksResult] from the API JSON envelope.
  factory BatchCreateLinksResult.fromJson(Map<String, dynamic> json) =>
      BatchCreateLinksResult(
        created: json['created'] as int? ?? 0,
        total: json['total'] as int? ?? 0,
        results:
            (json['results'] as List<dynamic>? ?? const <dynamic>[])
                .whereType<Map<String, dynamic>>()
                .map(BatchLinkResult.fromJson)
                .toList(growable: false),
      );

  /// Number of links successfully created.
  final int created;

  /// Total number of items in the batch.
  final int total;

  /// Per-item outcomes, in input order.
  final List<BatchLinkResult> results;
}
