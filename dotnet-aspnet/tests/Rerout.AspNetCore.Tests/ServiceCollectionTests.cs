using System;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Rerout;
using Rerout.AspNetCore;
using Rerout.AspNetCore.DependencyInjection;
using Xunit;

namespace Rerout.AspNetCore.Tests;

public sealed class ServiceCollectionTests
{
    [Fact]
    public void AddRerout_RegistersReroutClient()
    {
        var services = new ServiceCollection();
        services.AddRerout("rrk_test_key");

        using var provider = services.BuildServiceProvider();
        var client = provider.GetService<ReroutClient>();

        Assert.NotNull(client);
    }

    [Fact]
    public void AddRerout_RegistersClientAsSingleton()
    {
        var services = new ServiceCollection();
        services.AddRerout("rrk_test_key");

        using var provider = services.BuildServiceProvider();
        var first = provider.GetRequiredService<ReroutClient>();
        var second = provider.GetRequiredService<ReroutClient>();

        Assert.Same(first, second);
    }

    [Fact]
    public void AddRerout_WithBlankApiKey_Throws()
    {
        var services = new ServiceCollection();

        Assert.Throws<ArgumentException>(() => services.AddRerout("   "));
    }

    [Fact]
    public void AddRerout_WithOptions_AppliesCustomBaseUrl()
    {
        var services = new ServiceCollection();
        services.AddRerout(
            "rrk_test_key",
            new ReroutClientOptions { BaseUrl = "https://staging.rerout.co/" });

        using var provider = services.BuildServiceProvider();
        var client = provider.GetRequiredService<ReroutClient>();

        Assert.Equal("https://staging.rerout.co", client.BaseUrl);
    }

    [Fact]
    public void AddRerout_WithProviderOptionsFactory_ResolvesOptions()
    {
        var services = new ServiceCollection();
        services.AddRerout(
            "rrk_test_key",
            _ => new ReroutClientOptions { BaseUrl = "https://factory.rerout.co" });

        using var provider = services.BuildServiceProvider();
        var client = provider.GetRequiredService<ReroutClient>();

        Assert.Equal("https://factory.rerout.co", client.BaseUrl);
    }

    [Fact]
    public void AddReroutWebhooks_WithSecretString_ConfiguresOptions()
    {
        var services = new ServiceCollection();
        services.AddOptions();
        services.AddRerout("rrk_test_key").AddReroutWebhooks("whsec_abc");

        using var provider = services.BuildServiceProvider();
        var options = provider.GetRequiredService<IOptions<ReroutWebhookOptions>>().Value;

        Assert.Equal("whsec_abc", options.SigningSecret);
        Assert.Equal(300, options.ToleranceSeconds);
    }

    [Fact]
    public void AddReroutWebhooks_WithConfigureCallback_AppliesTolerance()
    {
        var services = new ServiceCollection();
        services.AddOptions();
        services.AddRerout("rrk_test_key").AddReroutWebhooks(options =>
        {
            options.SigningSecret = "whsec_xyz";
            options.ToleranceSeconds = 60;
        });

        using var provider = services.BuildServiceProvider();
        var options = provider.GetRequiredService<IOptions<ReroutWebhookOptions>>().Value;

        Assert.Equal("whsec_xyz", options.SigningSecret);
        Assert.Equal(60, options.ToleranceSeconds);
    }

    [Fact]
    public void AddReroutWebhookHandler_RegistersHandlerAsScoped()
    {
        var services = new ServiceCollection();
        services.AddRerout("rrk_test_key").AddReroutWebhookHandler<RecordingEventHandler>();

        var descriptor = Assert.Single(
            services,
            d => d.ServiceType == typeof(IReroutEventHandler));
        Assert.Equal(ServiceLifetime.Scoped, descriptor.Lifetime);
    }

    [Fact]
    public void AddReroutWebhookHandler_ResolvesHandlerInstance()
    {
        var services = new ServiceCollection();
        services.AddRerout("rrk_test_key").AddReroutWebhookHandler<RecordingEventHandler>();

        using var provider = services.BuildServiceProvider();
        using var scope = provider.CreateScope();
        var handler = scope.ServiceProvider.GetService<IReroutEventHandler>();

        Assert.NotNull(handler);
        Assert.IsType<RecordingEventHandler>(handler);
    }

    [Fact]
    public void AddRerout_IsIdempotent_DoesNotDuplicateClient()
    {
        var services = new ServiceCollection();
        services.AddRerout("rrk_first");
        services.AddRerout("rrk_second");

        var descriptor = Assert.Single(services, d => d.ServiceType == typeof(ReroutClient));
        Assert.Equal(ServiceLifetime.Singleton, descriptor.Lifetime);
    }

    [Fact]
    public void DefaultWebhookOptions_HaveBlankSecretAndDefaultTolerance()
    {
        var options = new ReroutWebhookOptions();

        Assert.Equal(string.Empty, options.SigningSecret);
        Assert.Equal(300, options.ToleranceSeconds);
    }
}
