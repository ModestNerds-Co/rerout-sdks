//
//  rerout
//  list_tags_result.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/tag_summary.dart';

/// The result of listing the project's tags.
@immutable
class ListTagsResult {
  /// Creates a [ListTagsResult].
  const ListTagsResult({required this.tags});

  /// Parses the listing from the API JSON envelope.
  factory ListTagsResult.fromJson(Map<String, dynamic> json) {
    final tags = (json['tags'] as List<dynamic>? ?? const <dynamic>[])
        .map((e) => TagSummary.fromJson(e as Map<String, dynamic>))
        .toList(growable: false);
    return ListTagsResult(tags: tags);
  }

  /// The project's tags, each carrying its live link count.
  final List<TagSummary> tags;
}
