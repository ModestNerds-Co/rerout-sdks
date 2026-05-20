//
//  rerout
//  create_link_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Request body for creating a short link.
class CreateLinkRequest {
  /// Creates a [CreateLinkRequest].
  const CreateLinkRequest({
    required this.targetUrl,
    this.domainHostname,
    this.code,
    this.expiresAt,
    this.seoTitle,
    this.seoDescription,
    this.seoImageUrl,
    this.seoCanonicalUrl,
    this.seoNoindex,
  });

  /// Absolute `https://` destination URL. Max 2048 characters.
  final String targetUrl;

  /// Verified custom domain to host this link on. Omit for `rerout.co/:code`.
  final String? domainHostname;

  /// Custom path. Only valid when [domainHostname] is provided.
  final String? code;

  /// Unix seconds — expiration. Omit for a permanent link.
  final int? expiresAt;

  /// Override social preview title. Max 90 characters.
  final String? seoTitle;

  /// Override social preview description. Max 220 characters.
  final String? seoDescription;

  /// Absolute `https://` social preview image URL.
  final String? seoImageUrl;

  /// Canonical URL for the preview HTML.
  final String? seoCanonicalUrl;

  /// Whether the preview page should be marked noindex. Defaults server-side.
  final bool? seoNoindex;

  /// Converts this request to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'target_url': targetUrl,
    if (domainHostname != null) 'domain_hostname': domainHostname,
    if (code != null) 'code': code,
    if (expiresAt != null) 'expires_at': expiresAt,
    if (seoTitle != null) 'seo_title': seoTitle,
    if (seoDescription != null) 'seo_description': seoDescription,
    if (seoImageUrl != null) 'seo_image_url': seoImageUrl,
    if (seoCanonicalUrl != null) 'seo_canonical_url': seoCanonicalUrl,
    if (seoNoindex != null) 'seo_noindex': seoNoindex,
  };
}
