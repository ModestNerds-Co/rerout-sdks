<?php

declare(strict_types=1);

return [

    /*
    |--------------------------------------------------------------------------
    | API Key
    |--------------------------------------------------------------------------
    |
    | The project API key (`rrk_…`) used to authenticate every request to the
    | Rerout API. Generate one in the Rerout dashboard. Required — the bound
    | client throws on construction if this is blank.
    |
    */

    'api_key' => env('REROUT_API_KEY'),

    /*
    |--------------------------------------------------------------------------
    | Base URL
    |--------------------------------------------------------------------------
    |
    | The Rerout API base URL. Leave null to use the production default
    | (https://api.rerout.co). Override for staging or self-hosted setups.
    |
    */

    'base_url' => env('REROUT_BASE_URL'),

    /*
    |--------------------------------------------------------------------------
    | Request Timeout
    |--------------------------------------------------------------------------
    |
    | Per-request timeout in seconds applied to every Rerout API call.
    |
    */

    'timeout' => (int) env('REROUT_TIMEOUT', 30),

    /*
    |--------------------------------------------------------------------------
    | Webhook
    |--------------------------------------------------------------------------
    |
    | Configuration for the bundled webhook controller.
    |
    | - secret:    Endpoint signing secret (`whsec_…`) used to verify the
    |              `X-Rerout-Signature` header on inbound deliveries.
    | - tolerance: Timestamp staleness window in seconds. Set 0 to disable the
    |              replay-protection check.
    | - route:     When `enabled` is true the package registers a POST route at
    |              `path` that runs the bundled WebhookController. Disable it to
    |              wire the controller into your own routes instead.
    |
    */

    'webhook' => [
        'secret' => env('REROUT_WEBHOOK_SECRET'),

        'tolerance' => (int) env('REROUT_WEBHOOK_TOLERANCE', 300),

        'route' => [
            'enabled' => (bool) env('REROUT_WEBHOOK_ROUTE_ENABLED', true),
            'path' => env('REROUT_WEBHOOK_ROUTE_PATH', 'rerout/webhook'),
            'middleware' => ['api'],
        ],
    ],

];
