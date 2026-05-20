//
//  rerout
//  result.dart
//
//  Official Dart SDK for the Rerout branded-link API.
//
//  Created by Ngonidzashe Mangudya on 20/05/2026.
//  Copyright (c) 2026 Codecraft Solutions
//  Licensed under the MIT License
//  https://codecraftsolutions.co.za
//

import 'package:meta/meta.dart';
import 'package:rerout/src/models/rerout_exception.dart';

/// A sealed class that represents either a successful result or an error.
///
/// Provides a functional approach to error handling without throwing.
///
/// ## Usage
///
/// ```dart
/// final result = await rerout.links.create(request);
/// switch (result) {
///   case Success(:final data):
///     print('Created ${data.shortUrl}');
///   case Error(:final error):
///     print('Failed: ${error.message}');
/// }
/// ```
sealed class Result<T> {
  const Result();

  /// Creates a successful result with the given data.
  factory Result.success(T data) = Success<T>;

  /// Creates an error result with the given exception.
  factory Result.error(ReroutException error) = Error<T>;

  /// Returns true if this is a successful result.
  bool get isSuccess => this is Success<T>;

  /// Returns true if this is an error result.
  bool get isError => this is Error<T>;

  /// Returns the data if successful, or null otherwise.
  T? get dataOrNull => switch (this) {
    Success(:final data) => data,
    Error() => null,
  };

  /// Returns the error if failed, or null otherwise.
  ReroutException? get errorOrNull => switch (this) {
    Success() => null,
    Error(:final error) => error,
  };

  /// Transforms the data if successful, otherwise returns the error.
  Result<R> map<R>(R Function(T) transform) => switch (this) {
    Success(:final data) => Result.success(transform(data)),
    Error(:final error) => Result.error(error),
  };

  /// Transforms the result using a function that returns another Result.
  Result<R> flatMap<R>(Result<R> Function(T) transform) => switch (this) {
    Success(:final data) => transform(data),
    Error(:final error) => Result.error(error),
  };

  /// Executes the appropriate callback based on the result type.
  R when<R>({
    required R Function(T data) success,
    required R Function(ReroutException error) error,
  }) {
    return switch (this) {
      Success(:final data) => success(data),
      Error(error: final e) => error(e),
    };
  }
}

/// Represents a successful result containing data of type [T].
@immutable
final class Success<T> extends Result<T> {
  /// Creates a successful result containing [data].
  const Success(this.data);

  /// The successful result data.
  final T data;

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is Success<T> && data == other.data;

  @override
  int get hashCode => data.hashCode;

  @override
  String toString() => 'Success($data)';
}

/// Represents an error result containing a [ReroutException].
@immutable
final class Error<T> extends Result<T> {
  /// Creates an error result containing [error].
  const Error(this.error);

  /// The error that occurred.
  final ReroutException error;

  @override
  bool operator ==(Object other) =>
      identical(this, other) || other is Error<T> && error == other.error;

  @override
  int get hashCode => error.hashCode;

  @override
  String toString() => 'Error($error)';
}
