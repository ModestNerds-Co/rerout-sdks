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
import 'package:rerout/src/models/ab_variant.dart';
import 'package:rerout/src/models/routing_rule.dart';
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
    this.passwordProtected = false,
    this.clickCount = 0,
    this.trackConversions = false,
    this.routingRules = const [],
    this.abVariants = const [],
    this.maxClicks,
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
    passwordProtected: json['password_protected'] as bool? ?? false,
    clickCount: json['click_count'] as int? ?? 0,
    trackConversions: json['track_conversions'] as bool? ?? false,
    routingRules:
        (json['routing_rules'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<Map<String, dynamic>>()
            .map(RoutingRule.fromJson)
            .toList(growable: false),
    abVariants:
        (json['ab_variants'] as List<dynamic>? ?? const <dynamic>[])
            .whereType<Map<String, dynamic>>()
            .map(AbVariant.fromJson)
            .toList(growable: false),
    maxClicks: json['max_clicks'] as int?,
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

  /// Smart Links: whether a password is required to follow this link.
  final bool passwordProtected;

  /// Smart Links: total clicks recorded against this link.
  final int clickCount;

  /// Smart Links: whether conversion tracking is enabled.
  final bool trackConversions;

  /// Smart Links: ordered geo/device routing rules.
  final List<RoutingRule> routingRules;

  /// Smart Links: weighted A/B destinations.
  final List<AbVariant> abVariants;

  /// Smart Links: click cap, or null when uncapped.
  final int? maxClicks;

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
          passwordProtected == other.passwordProtected &&
          clickCount == other.clickCount &&
          trackConversions == other.trackConversions &&
          _listEqual(routingRules, other.routingRules) &&
          _listEqual(abVariants, other.abVariants) &&
          maxClicks == other.maxClicks &&
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
    passwordProtected,
    clickCount,
    trackConversions,
    Object.hashAll(routingRules),
    Object.hashAll(abVariants),
    maxClicks,
    domainHostname,
    expiresAt,
    seoTitle,
    Object.hash(
      seoDescription,
      seoImageUrl,
      seoCanonicalUrl,
      seoUpdatedAt,
    ),
  );

  @override
  String toString() => 'ShortLink(code: $code, shortUrl: $shortUrl)';
}

bool _tagsEqual(List<Tag> a, List<Tag> b) => _listEqual(a, b);

bool _listEqual<T>(List<T> a, List<T> b) {
  if (identical(a, b)) return true;
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
