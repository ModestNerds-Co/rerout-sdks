<?php

declare(strict_types=1);

namespace Rerout\Laravel\Console;

use Illuminate\Console\Command;
use Rerout\Exceptions\ReroutException;
use Rerout\Rerout as ReroutClient;

/**
 * `php artisan rerout:ping` — verifies the configured API key by calling
 * `GET /v1/projects/me` and printing the resolved project.
 *
 * Exits 0 on success, 1 on any Rerout API failure.
 */
final class PingCommand extends Command
{
    /**
     * @var string
     */
    protected $signature = 'rerout:ping';

    /**
     * @var string
     */
    protected $description = 'Verify the Rerout API key by fetching the current project.';

    /**
     * Execute the console command.
     */
    public function handle(ReroutClient $rerout): int
    {
        try {
            $project = $rerout->project()->me();
        } catch (ReroutException $e) {
            $this->error(sprintf(
                'Rerout ping failed [%s, status %d]: %s',
                $e->code(),
                $e->status(),
                $e->getMessage(),
            ));

            return self::FAILURE;
        }

        $name = is_string($project['name'] ?? null) ? $project['name'] : '(unknown)';
        $slug = is_string($project['slug'] ?? null) ? $project['slug'] : '(unknown)';

        $this->info(sprintf('Connected to Rerout project "%s" (%s).', $name, $slug));

        return self::SUCCESS;
    }
}
