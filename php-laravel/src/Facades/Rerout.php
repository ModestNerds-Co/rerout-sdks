<?php

declare(strict_types=1);

namespace Rerout\Laravel\Facades;

use Illuminate\Support\Facades\Facade;
use Rerout\Rerout as ReroutClient;

/**
 * Facade for the shared {@see ReroutClient} singleton.
 *
 * ```php
 * use Rerout\Laravel\Facades\Rerout;
 * use Rerout\Models\CreateLinkInput;
 *
 * $link = Rerout::links()->create(new CreateLinkInput(
 *     targetUrl: 'https://example.com',
 * ));
 * ```
 *
 * @method static \Rerout\Resources\Links   links()
 * @method static \Rerout\Resources\Project project()
 * @method static \Rerout\Resources\Qr      qr()
 * @method static string                    baseUrl()
 *
 * @see ReroutClient
 */
final class Rerout extends Facade
{
    /**
     * The container binding the facade resolves to.
     */
    protected static function getFacadeAccessor(): string
    {
        return ReroutClient::class;
    }
}
