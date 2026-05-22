using System;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Rerout.AspNetCore;

namespace Rerout.AspNetCore.Tests;

/// <summary>
/// A <see cref="WebApplicationFactory{TEntryPoint}"/> for the webhook test app
/// that lets each test pick a signing secret, tolerance, and handler behaviour.
/// </summary>
internal sealed class ReroutWebhookFactory : WebApplicationFactory<Program>
{
    private string _secret = "whsec_test_secret";
    private int _tolerance = 300;
    private bool _registerHandler = true;
    private Action<RecordingEventHandler>? _configureHandler;

    /// <summary>Use this signing secret for the webhook endpoint.</summary>
    public ReroutWebhookFactory WithSecret(string secret)
    {
        _secret = secret;
        return this;
    }

    /// <summary>Use this timestamp tolerance (seconds).</summary>
    public ReroutWebhookFactory WithTolerance(int seconds)
    {
        _tolerance = seconds;
        return this;
    }

    /// <summary>Skip registering an <see cref="IReroutEventHandler"/> entirely.</summary>
    public ReroutWebhookFactory WithoutHandler()
    {
        _registerHandler = false;
        return this;
    }

    /// <summary>Mutate the recording handler before the host starts (e.g. arm a throw).</summary>
    public ReroutWebhookFactory ConfigureHandler(Action<RecordingEventHandler> configure)
    {
        _configureHandler = configure;
        return this;
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        // The test app has no static assets; point the content root at the
        // build output so the host's file provider has a directory to bind.
        builder.UseContentRoot(AppContext.BaseDirectory);
        builder.UseEnvironment("Development");

        builder.ConfigureServices(services =>
        {
            // Replace the webhook options with the per-test values.
            services.Configure<ReroutWebhookOptions>(options =>
            {
                options.SigningSecret = _secret;
                options.ToleranceSeconds = _tolerance;
            });

            var recorder = new RecordingEventHandler();
            _configureHandler?.Invoke(recorder);

            // Swap in the test-controlled recorder singleton.
            services.RemoveAll<RecordingEventHandler>();
            services.AddSingleton(recorder);

            services.RemoveAll<IReroutEventHandler>();
            if (_registerHandler)
            {
                services.AddScoped<IReroutEventHandler>(_ => recorder);
            }
        });
    }

    /// <summary>The recording handler the running host resolves.</summary>
    public RecordingEventHandler Recorder => Services.GetRequiredService<RecordingEventHandler>();
}
