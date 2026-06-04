//
//  rerout
//  update_link_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:rerout/src/models/ab_variant.dart';
import 'package:rerout/src/models/routing_rule.dart';

/// Request body for the `PATCH /v1/links/:code` endpoint. Only fields set on
/// the instance are sent — server-side merge semantics apply.
class UpdateLinkRequest {
  /// Creates an [UpdateLinkRequest].
  const UpdateLinkRequest({
    this.targetUrl,
    this.expiresAt,
    this.clearExpiresAt = false,
    this.isActive,
    this.seoTitle,
    this.clearSeoTitle = false,
    this.seoDescription,
    this.clearSeoDescription = false,
    this.seoImageUrl,
    this.clearSeoImageUrl = false,
    this.seoCanonicalUrl,
    this.clearSeoCanonicalUrl = false,
    this.seoNoindex,
    this.password,
    this.clearPassword = false,
    this.maxClicks,
    this.clearMaxClicks = false,
    this.trackConversions,
    this.routingRules,
    this.abVariants,
  });

  /// New destination URL.
  final String? targetUrl;

  /// New expiry (unix seconds). Use [clearExpiresAt] to remove an existing one.
  final int? expiresAt;

  /// Set to true to remove an existing expiry — sends `expires_at: null`.
  final bool clearExpiresAt;

  /// Activate or deactivate the link.
  final bool? isActive;

  /// New preview title.
  final String? seoTitle;

  /// Set to true to clear an existing SEO title.
  final bool clearSeoTitle;

  /// New preview description.
  final String? seoDescription;

  /// Set to true to clear an existing SEO description.
  final bool clearSeoDescription;

  /// New preview image URL.
  final String? seoImageUrl;

  /// Set to true to clear an existing SEO image URL.
  final bool clearSeoImageUrl;

  /// New canonical URL.
  final String? seoCanonicalUrl;

  /// Set to true to clear an existing SEO canonical URL.
  final bool clearSeoCanonicalUrl;

  /// Toggle whether the preview page is noindex.
  final bool? seoNoindex;

  /// New Smart Links password. Use [clearPassword] to remove an existing one.
  final String? password;

  /// Set to true to clear an existing password — sends `password: null`.
  final bool clearPassword;

  /// New click cap. Use [clearMaxClicks] to remove an existing cap.
  final int? maxClicks;

  /// Set to true to remove the click cap — sends `max_clicks: null`.
  final bool clearMaxClicks;

  /// Toggle Smart Links conversion tracking.
  final bool? trackConversions;

  /// Full-replace the routing rules when set.
  final List<RoutingRule>? routingRules;

  /// Full-replace the A/B variants when set.
  final List<AbVariantInput>? abVariants;

  /// Converts this request to the JSON map expected by the API. Fields are
  /// only included when they were set — explicit nulls happen via the
  /// `clear*` flags so an unset field doesn't accidentally wipe server state.
  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (targetUrl != null) json['target_url'] = targetUrl;
    if (clearExpiresAt) {
      json['expires_at'] = null;
    } else if (expiresAt != null) {
      json['expires_at'] = expiresAt;
    }
    if (isActive != null) json['is_active'] = isActive;
    if (clearSeoTitle) {
      json['seo_title'] = null;
    } else if (seoTitle != null) {
      json['seo_title'] = seoTitle;
    }
    if (clearSeoDescription) {
      json['seo_description'] = null;
    } else if (seoDescription != null) {
      json['seo_description'] = seoDescription;
    }
    if (clearSeoImageUrl) {
      json['seo_image_url'] = null;
    } else if (seoImageUrl != null) {
      json['seo_image_url'] = seoImageUrl;
    }
    if (clearSeoCanonicalUrl) {
      json['seo_canonical_url'] = null;
    } else if (seoCanonicalUrl != null) {
      json['seo_canonical_url'] = seoCanonicalUrl;
    }
    if (seoNoindex != null) json['seo_noindex'] = seoNoindex;
    if (clearPassword) {
      json['password'] = null;
    } else if (password != null) {
      json['password'] = password;
    }
    if (clearMaxClicks) {
      json['max_clicks'] = null;
    } else if (maxClicks != null) {
      json['max_clicks'] = maxClicks;
    }
    if (trackConversions != null) json['track_conversions'] = trackConversions;
    if (routingRules != null) {
      json['routing_rules'] = routingRules!.map((r) => r.toJson()).toList();
    }
    if (abVariants != null) {
      json['ab_variants'] = abVariants!.map((v) => v.toJson()).toList();
    }
    return json;
  }

  /// True when no field is set — the API would reject this as a no-op.
  bool get isEmpty => toJson().isEmpty;
}
