<?php

declare(strict_types=1);

use Illuminate\Support\Facades\Route;
use Rerout\Laravel\Http\WebhookController;

/*
|--------------------------------------------------------------------------
| Rerout webhook route
|--------------------------------------------------------------------------
|
| Registered by ReroutServiceProvider when `rerout.webhook.route.enabled` is
| true. The path and middleware are taken from `config/rerout.php`.
|
*/

$path = config('rerout.webhook.route.path', 'rerout/webhook');
$path = is_string($path) ? $path : 'rerout/webhook';

$middleware = config('rerout.webhook.route.middleware', ['api']);
$middleware = is_array($middleware) ? $middleware : ['api'];

Route::middleware($middleware)
    ->post($path, WebhookController::class)
    ->name('rerout.webhook');
