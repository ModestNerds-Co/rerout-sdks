//
//  rerout
//  update_tag_request.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

/// Request body for the `PATCH /v1/projects/me/tags/:tag_id` endpoint. Only
/// fields set on the instance are sent — an omitted field is left unchanged.
///
/// Both fields are non-nullable on the server, so there is no "clear" variant
/// (unlike `UpdateLinkRequest`, whose optional fields can be nulled out).
class UpdateTagRequest {
  /// Creates an [UpdateTagRequest].
  const UpdateTagRequest({this.name, this.color});

  /// New tag label. Omit to leave unchanged.
  final String? name;

  /// New display color. Omit to leave unchanged.
  final String? color;

  /// Converts this request to the JSON map expected by the API. Fields are
  /// only included when they were set, so an unset field doesn't touch server
  /// state.
  Map<String, dynamic> toJson() => {
    if (name != null) 'name': name,
    if (color != null) 'color': color,
  };

  /// True when no field is set — the API would reject this as a no-op.
  bool get isEmpty => toJson().isEmpty;
}
