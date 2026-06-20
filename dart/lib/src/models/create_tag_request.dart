//
//  rerout
//  create_tag_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Request body for creating a tag (`POST /v1/projects/me/tags`).
class CreateTagRequest {
  /// Creates a [CreateTagRequest].
  const CreateTagRequest({required this.name, this.color});

  /// Tag label. Required.
  final String name;

  /// Display color. Optional — the server validates it against its palette and
  /// defaults to `teal` when omitted.
  final String? color;

  /// Converts this request to the JSON map expected by the API.
  Map<String, dynamic> toJson() => {
    'name': name,
    if (color != null) 'color': color,
  };
}
