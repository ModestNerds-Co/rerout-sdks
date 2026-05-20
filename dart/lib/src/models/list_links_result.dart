//
//  rerout
//  list_links_result.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/short_link.dart';

/// A paginated page of short links.
@immutable
class ListLinksResult {
  /// Creates a [ListLinksResult].
  const ListLinksResult({required this.links, this.nextCursor});

  /// Parses a page from the API JSON envelope.
  factory ListLinksResult.fromJson(Map<String, dynamic> json) {
    final raw = (json['links'] as List<dynamic>? ?? const <dynamic>[])
        .map((e) => ShortLink.fromJson(e as Map<String, dynamic>))
        .toList(growable: false);
    return ListLinksResult(
      links: raw,
      nextCursor: json['next_cursor'] as int?,
    );
  }

  /// Links on this page, newest first.
  final List<ShortLink> links;

  /// Cursor for the next page, or null when this is the last page.
  final int? nextCursor;

  /// Whether more pages remain.
  bool get hasMore => nextCursor != null;
}
