using System;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Rerout;

namespace Rerout.AspNetCore.DependencyInjection;

/// <summary>
/// Dependency-injection helpers for registering the Rerout SDK in an
/// ASP.NET Core application.
/// </summary>
public static class ReroutServiceCollectionExtensions
{
    /// <summary>
    /// Register a singleton <see cref="ReroutClient"/> built from the supplied
    /// API key with default client options.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <param name="apiKey">Project API key (<c>rrk_…</c>). Required.</param>
    /// <returns>A <see cref="IReroutBuilder"/> for chaining webhook setup.</returns>
    public static IReroutBuilder AddRerout(this IServiceCollection services, string apiKey)
    {
        return services.AddRerout(apiKey, _ => new ReroutClientOptions());
    }

    /// <summary>
    /// Register a singleton <see cref="ReroutClient"/> with the supplied API key
    /// and explicit client options — base URL, timeout, or an injected
    /// <see cref="System.Net.Http.HttpClient"/>.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <param name="apiKey">Project API key (<c>rrk_…</c>). Required.</param>
    /// <param name="options">The client transport options.</param>
    public static IReroutBuilder AddRerout(
        this IServiceCollection services,
        string apiKey,
        ReroutClientOptions options)
    {
        ArgumentNullException.ThrowIfNull(options);
        return services.AddRerout(apiKey, _ => options);
    }

    /// <summary>
    /// Register a singleton <see cref="ReroutClient"/> whose options are built
    /// from the application's <see cref="IServiceProvider"/> — useful when the
    /// base URL or timeout come from configuration.
    /// </summary>
    /// <param name="services">The service collection.</param>
    /// <param name="apiKey">Project API key (<c>rrk_…</c>). Required.</param>
    /// <param name="optionsFactory">Builds the client options from the provider.</param>
    public static IReroutBuilder AddRerout(
        this IServiceCollection services,
        string apiKey,
        Func<IServiceProvider, ReroutClientOptions> optionsFactory)
    {
        ArgumentNullException.ThrowIfNull(services);
        ArgumentNullException.ThrowIfNull(optionsFactory);

        if (string.IsNullOrWhiteSpace(apiKey))
        {
            throw new ArgumentException("A Rerout project API key is required.", nameof(apiKey));
        }

        services.TryAddSingleton(provider => new ReroutClient(apiKey, optionsFactory(provider)));

        return new ReroutBuilder(services);
    }

    /// <summary>
    /// Configure the webhook signing secret and tolerance for
    /// <c>MapReroutWebhook</c>.
    /// </summary>
    public static IReroutBuilder AddReroutWebhooks(
        this IReroutBuilder builder,
        Action<ReroutWebhookOptions> configure)
    {
        ArgumentNullException.ThrowIfNull(builder);
        ArgumentNullException.ThrowIfNull(configure);

        builder.Services.AddOptions();
        builder.Services.Configure(configure);
        return builder;
    }

    /// <summary>
    /// Configure the webhook signing secret directly from a string. The
    /// timestamp tolerance keeps its default of five minutes.
    /// </summary>
    public static IReroutBuilder AddReroutWebhooks(this IReroutBuilder builder, string signingSecret)
    {
        return builder.AddReroutWebhooks(options => options.SigningSecret = signingSecret);
    }

    /// <summary>
    /// Register the scoped <see cref="IReroutEventHandler"/> implementation the
    /// webhook middleware dispatches verified events to.
    /// </summary>
    /// <typeparam name="THandler">The handler type.</typeparam>
    public static IReroutBuilder AddReroutWebhookHandler<THandler>(this IReroutBuilder builder)
        where THandler : class, IReroutEventHandler
    {
        ArgumentNullException.ThrowIfNull(builder);

        builder.Services.AddScoped<IReroutEventHandler, THandler>();
        return builder;
    }
}

/// <summary>Fluent builder returned by <c>AddRerout</c> for chaining webhook setup.</summary>
public interface IReroutBuilder
{
    /// <summary>The underlying service collection.</summary>
    IServiceCollection Services { get; }
}

internal sealed class ReroutBuilder : IReroutBuilder
{
    public ReroutBuilder(IServiceCollection services) => Services = services;

    public IServiceCollection Services { get; }
}
