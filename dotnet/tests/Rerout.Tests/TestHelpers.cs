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
          "updated_at": 1716200000
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
