//
//  rerout
//  tag.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';

/// A tag attached to a short link, as returned by the Rerout API.
@immutable
class Tag {
  /// Creates a [Tag].
  const Tag({required this.id, required this.name, required this.color});

  /// Parses a [Tag] from the API JSON envelope.
  factory Tag.fromJson(Map<String, dynamic> json) => Tag(
    id: json['id'] as String,
    name: json['name'] as String,
    color: json['color'] as String,
  );

  /// Stable tag identifier.
  final String id;

  /// Human-readable tag label.
  final String name;

  /// Display color — typically a hex string such as `#FF8800`.
  final String color;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Tag &&
          id == other.id &&
          name == other.name &&
          color == other.color;

  @override
  int get hashCode => Object.hash(id, name, color);

  @override
  String toString() => 'Tag(id: $id, name: $name, color: $color)';
}
