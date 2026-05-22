using System.Collections.Generic;

namespace Rerout.Models;

/// <summary>
/// QR rendering parameters for <see cref="Resources.Qr.Url"/> and
/// <see cref="Resources.Qr.SvgAsync"/>.
/// </summary>
/// <remarks>
/// All fields are optional — omit any to use the server default.
/// </remarks>
public sealed record QrOptions
{
    /// <summary>Module size in pixels. 1–32. Server default: 8.</summary>
    public int? Size { get; init; }

    /// <summary>Quiet-zone modules. 0–16. Server default: 4.</summary>
    public int? Margin { get; init; }

    /// <summary>Error-correction level — <c>L</c>, <c>M</c>, <c>Q</c>, or <c>H</c>.</summary>
    public string? Ecc { get; init; }

    /// <summary>
    /// Force the QR to encode a specific verified custom domain instead of the
    /// project's default branding domain.
    /// </summary>
    public string? Domain { get; init; }

    /// <summary>
    /// Cache-bust token. Setting <see cref="RefreshAlways"/> sends
    /// <c>refresh=1</c>; any other non-empty string is forwarded verbatim and
    /// triggers a fresh render server-side.
    /// </summary>
    public string? Refresh { get; init; }

    /// <summary>
    /// Whether to force a fresh render. When <c>true</c>, emits <c>refresh=1</c>
    /// unless an explicit <see cref="Refresh"/> token is also supplied (the
    /// token wins).
    /// </summary>
    public bool RefreshAlways { get; init; }

    /// <summary>Render this options bag into URL query pairs.</summary>
    internal IReadOnlyList<KeyValuePair<string, string>> ToQueryParameters()
    {
        var pairs = new List<KeyValuePair<string, string>>();
        if (Size is { } size)
        {
            pairs.Add(new KeyValuePair<string, string>("size", size.ToString(System.Globalization.CultureInfo.InvariantCulture)));
        }

        if (Margin is { } margin)
        {
            pairs.Add(new KeyValuePair<string, string>("margin", margin.ToString(System.Globalization.CultureInfo.InvariantCulture)));
        }

        if (!string.IsNullOrEmpty(Ecc))
        {
            pairs.Add(new KeyValuePair<string, string>("ecc", Ecc));
        }

        if (!string.IsNullOrEmpty(Domain))
        {
            pairs.Add(new KeyValuePair<string, string>("domain", Domain));
        }

        if (!string.IsNullOrEmpty(Refresh))
        {
            pairs.Add(new KeyValuePair<string, string>("refresh", Refresh));
        }
        else if (RefreshAlways)
        {
            pairs.Add(new KeyValuePair<string, string>("refresh", "1"));
        }

        return pairs;
    }
}
