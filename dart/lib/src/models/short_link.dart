//
//  rerout
//  short_link.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/tag.dart';

/// A short link as returned by the Rerout API.
@immutable
class ShortLink {
  /// Creates a [ShortLink].
  const ShortLink({
    required this.code,
    required this.shortUrl,
    required this.targetUrl,
    required this.projectId,
    required this.isActive,
    required this.seoNoindex,
    required this.createdAt,
    required this.updatedAt,
    this.tags = const [],
    this.domainHostname,
    this.expiresAt,
    this.seoTitle,
    this.seoDescription,
    this.seoImageUrl,
    this.seoCanonicalUrl,
    this.seoUpdatedAt,
  });

  /// Parses a [ShortLink] from the API JSON envelope.
  factory ShortLink.fromJson(Map<String, dynamic> json) => ShortLink(
    code: json['code'] as String,
    shortUrl: json['short_url'] as String,
    targetUrl: json['target_url'] as String,
    projectId: json['project_id'] as String,
    isActive: json['is_active'] as bool,
    seoNoindex: json['seo_noindex'] as bool? ?? true,
    createdAt: json['created_at'] as int,
    updatedAt: json['updated_at'] as int,
    tags:
        (json['tags'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<Map<String, dynamic>>()
            .map(Tag.fromJson)
            .toList(growable: false),
    domainHostname: json['domain_hostname'] as String?,
    expiresAt: json['expires_at'] as int?,
    seoTitle: json['seo_title'] as String?,
    seoDescription: json['seo_description'] as String?,
    seoImageUrl: json['seo_image_url'] as String?,
    seoCanonicalUrl: json['seo_canonical_url'] as String?,
    seoUpdatedAt: json['seo_updated_at'] as int?,
  );

  /// The short link path code.
  final String code;

  /// Fully-qualified short URL — `https://{host}/{code}`.
  final String shortUrl;

  /// Destination the redirect resolves to.
  final String targetUrl;

  /// Project that owns the link.
  final String projectId;

  /// Whether the link is currently active.
  final bool isActive;

  /// Whether the preview HTML should be indexed.
  final bool seoNoindex;

  /// Unix seconds — link creation time.
  final int createdAt;

  /// Unix seconds — last mutation.
  final int updatedAt;

  /// Tags attached to the link. Empty on create; populated on get/list/update.
  final List<Tag> tags;

  /// Verified custom domain hosting this link, when one is bound.
  final String? domainHostname;

  /// Unix seconds — expiration. Null for permanent links.
  final int? expiresAt;

  /// Override social preview title.
  final String? seoTitle;

  /// Override social preview description.
  final String? seoDescription;

  /// Override social preview image URL.
  final String? seoImageUrl;

  /// Override preview canonical URL.
  final String? seoCanonicalUrl;

  /// Unix seconds — last SEO field change.
  final int? seoUpdatedAt;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ShortLink &&
          code == other.code &&
          shortUrl == other.shortUrl &&
          targetUrl == other.targetUrl &&
          projectId == other.projectId &&
          isActive == other.isActive &&
          seoNoindex == other.seoNoindex &&
          createdAt == other.createdAt &&
          updatedAt == other.updatedAt &&
          _tagsEqual(tags, other.tags) &&
          domainHostname == other.domainHostname &&
          expiresAt == other.expiresAt &&
          seoTitle == other.seoTitle &&
          seoDescription == other.seoDescription &&
          seoImageUrl == other.seoImageUrl &&
          seoCanonicalUrl == other.seoCanonicalUrl &&
          seoUpdatedAt == other.seoUpdatedAt;

  @override
  int get hashCode => Object.hash(
    code,
    shortUrl,
    targetUrl,
    projectId,
    isActive,
    seoNoindex,
    createdAt,
    updatedAt,
    Object.hashAll(tags),
    domainHostname,
    expiresAt,
    seoTitle,
    seoDescription,
    seoImageUrl,
    seoCanonicalUrl,
    seoUpdatedAt,
  );

  @override
  String toString() => 'ShortLink(code: $code, shortUrl: $shortUrl)';
}

bool _tagsEqual(List<Tag> a, List<Tag> b) {
  if (identical(a, b)) return true;
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
