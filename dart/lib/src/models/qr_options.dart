//
//  rerout
//  qr_options.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// QR rendering parameters for `Rerout.qr.url()` / `Rerout.qr.svg()`.
///
/// All fields are optional — omit any to use the server default.
class QrOptions {
  /// Creates [QrOptions].
  const QrOptions({
    this.size,
    this.margin,
    this.ecc,
    this.domain,
    this.refresh,
  });

  /// Module size in pixels. 1–32. Server default: 8.
  final int? size;

  /// Quiet-zone modules. 0–16. Server default: 4.
  final int? margin;

  /// Error-correction level — `L`, `M`, `Q`, or `H`.
  final String? ecc;

  /// Force the QR to encode a specific verified custom domain instead of
  /// the project's default branding domain.
  final String? domain;

  /// Cache-bust token. Passing `true` sends `refresh=1`; any non-empty string
  /// is forwarded verbatim and triggers a fresh render server-side.
  final Object? refresh;

  /// Render this options bag into URL query pairs.
  Map<String, String> toQueryParameters() {
    final out = <String, String>{};
    if (size != null) out['size'] = size!.toString();
    if (margin != null) out['margin'] = margin!.toString();
    if (ecc != null) out['ecc'] = ecc!;
    if (domain != null) out['domain'] = domain!;
    final refresh = this.refresh;
    if (refresh != null) {
      out['refresh'] = refresh == true ? '1' : refresh.toString();
    }
    return out;
  }
}
