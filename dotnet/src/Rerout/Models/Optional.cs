using System;
using System.Collections.Generic;

namespace Rerout.Models;

/// <summary>
/// Three-state value used by <see cref="UpdateLinkInput"/> to distinguish between
/// "leave the field alone", "set the field to a value", and "explicitly clear
/// the field on the server".
/// </summary>
/// <remarks>
/// <para>
/// PATCH semantics need to express more than the nullable type allows. The
/// default value (<see cref="Unset"/>) means the field is omitted from the
/// payload entirely. <see cref="Set(T)"/> sends the supplied value, including
/// <c>null</c> when <typeparamref name="T"/> is a nullable reference type —
/// which the server interprets as "clear this field".
/// </para>
/// <para>
/// <c>Optional&lt;string?&gt;.Set(null)</c> ⇒ sends <c>"field": null</c><br/>
/// <c>Optional&lt;string?&gt;.Set("hi")</c> ⇒ sends <c>"field": "hi"</c><br/>
/// <c>default(Optional&lt;string?&gt;)</c> or <see cref="Unset"/> ⇒ field omitted.
/// </para>
/// </remarks>
public readonly struct Optional<T> : IEquatable<Optional<T>>
{
    private readonly T _value;

    /// <summary>The unset sentinel — leave the field alone.</summary>
    public static Optional<T> Unset => default;

    private Optional(T value, bool hasValue)
    {
        _value = value;
        HasValue = hasValue;
    }

    /// <summary>Wrap a value, signalling that it should be sent to the server.</summary>
    public static Optional<T> Set(T value) => new(value, true);

    /// <summary><c>true</c> when this optional carries a value to send.</summary>
    public bool HasValue { get; }

    /// <summary>
    /// The wrapped value. Inspect <see cref="HasValue"/> first — calling this
    /// when <see cref="HasValue"/> is <c>false</c> returns <c>default(T)</c>.
    /// </summary>
    public T Value => _value;

    /// <summary>Try-get accessor for the wrapped value.</summary>
    public bool TryGetValue(out T value)
    {
        value = _value;
        return HasValue;
    }

    /// <summary>Implicit conversion from <typeparamref name="T"/> via <see cref="Set(T)"/>.</summary>
    public static implicit operator Optional<T>(T value) => Set(value);

    /// <inheritdoc />
    public bool Equals(Optional<T> other) =>
        HasValue == other.HasValue && EqualityComparer<T>.Default.Equals(_value, other._value);

    /// <inheritdoc />
    public override bool Equals(object? obj) => obj is Optional<T> other && Equals(other);

    /// <inheritdoc />
    public override int GetHashCode() =>
        HasValue ? HashCode.Combine(true, _value) : HashCode.Combine(false);

    /// <inheritdoc />
    public override string ToString() => HasValue ? $"Set({_value})" : "Unset";

    /// <summary>Equality operator.</summary>
    public static bool operator ==(Optional<T> left, Optional<T> right) => left.Equals(right);

    /// <summary>Inequality operator.</summary>
    public static bool operator !=(Optional<T> left, Optional<T> right) => !left.Equals(right);
}
