using System.Net.Http;
using Rerout;

namespace Rerout.Tests;

/// <summary>Shared helpers for building a client wired to a stub handler.</summary>
internal static class TestHelpers
{
    public const string ApiKey = "rrk_test_key";

    /// <summary>A complete <see cref="Link"/> JSON payload, for success-path tests.</summary>
    public const string SampleLinkJson = """
        {
          "code": "q4",
          "short_url": "https://go.brand.com/q4",
          "domain_hostname": "go.brand.com",
          "target_url": "https://example.com/q4-sale",
          "project_id": "proj_123",
          "expires_at": null,
          "is_active": true,
          "seo_title": "Q4 Sale",
          "seo_description": null,
          "seo_image_url": null,
          "seo_canonical_url": null,
          "seo_noindex": false,
          "seo_updated_at": null,
          "created_at": 1716200000,
          "updated_at": 1716200000,
          "tags": [
            { "id": "tag_1", "name": "campaign", "color": "#ff8800" }
          ]
        }
        """;

    /// <summary>A <see cref="Link"/> JSON payload with no <c>tags</c> key, for lenient-parse tests.</summary>
    public const string SampleLinkJsonWithoutTags = """
        {
          "code": "q4",
          "short_url": "https://go.brand.com/q4",
          "domain_hostname": "go.brand.com",
          "target_url": "https://example.com/q4-sale",
          "project_id": "proj_123",
          "expires_at": null,
          "is_active": true,
          "seo_title": "Q4 Sale",
          "seo_description": null,
          "seo_image_url": null,
          "seo_canonical_url": null,
          "seo_noindex": false,
          "seo_updated_at": null,
          "created_at": 1716200000,
          "updated_at": 1716200000
        }
        """;

    /// <summary>A <see cref="Link"/> JSON payload carrying the Smart Links fields.</summary>
    public const string SampleSmartLinkJson = """
        {
          "code": "vip",
          "short_url": "https://go.brand.com/vip",
          "domain_hostname": "go.brand.com",
          "target_url": "https://example.com/default",
          "project_id": "proj_123",
          "expires_at": null,
          "is_active": true,
          "seo_title": null,
          "seo_description": null,
          "seo_image_url": null,
          "seo_canonical_url": null,
          "seo_noindex": false,
          "seo_updated_at": null,
          "created_at": 1716200000,
          "updated_at": 1716200000,
          "tags": [],
          "password_protected": true,
          "max_clicks": 1000,
          "click_count": 42,
          "track_conversions": true,
          "routing_rules": [
            { "condition_type": "country", "condition_op": "is", "condition_value": "ZA", "target_url": "https://example.com/za" },
            { "condition_type": "device", "condition_op": "in", "condition_value": "mobile,tablet", "target_url": "https://example.com/m" }
          ],
          "ab_variants": [
            { "id": 1, "target_url": "https://example.com/a", "weight": 60 },
            { "id": 2, "target_url": "https://example.com/b", "weight": 40 }
          ]
        }
        """;

    /// <summary>Build a client whose transport is the supplied stub handler.</summary>
    public static ReroutClient ClientWith(
        StubHttpMessageHandler handler,
        string baseUrl = "https://api.rerout.co")
    {
        var http = new HttpClient(handler);
        return new ReroutClient(ApiKey, new ReroutClientOptions
        {
            BaseUrl = baseUrl,
            HttpClient = http,
        });
    }
}
