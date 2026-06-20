//
//  rerout
//  tag_summary.dart
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

/// A tag together with the number of live (non-deleted) links it is attached
/// to. Returned by `tags.list()`; the plain [Tag] returned by create/update
/// omits the count.
@immutable
class TagSummary extends Tag {
  /// Creates a [TagSummary].
  const TagSummary({
    required super.id,
    required super.name,
    required super.color,
    required this.linkCount,
  });

  /// Parses a [TagSummary] from the API JSON envelope.
  factory TagSummary.fromJson(Map<String, dynamic> json) => TagSummary(
    id: json['id'] as String,
    name: json['name'] as String,
    color: json['color'] as String,
    linkCount: (json['link_count'] as num?)?.toInt() ?? 0,
  );

  /// Number of live links currently carrying this tag.
  final int linkCount;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TagSummary &&
          id == other.id &&
          name == other.name &&
          color == other.color &&
          linkCount == other.linkCount;

  @override
  int get hashCode => Object.hash(id, name, color, linkCount);

  @override
  String toString() =>
      'TagSummary(id: $id, name: $name, color: $color, linkCount: $linkCount)';
}
