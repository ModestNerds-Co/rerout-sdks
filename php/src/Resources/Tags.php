<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Exceptions\ReroutException;
use Rerout\Models\CreateTagInput;
use Rerout\Models\ListTagsResult;
use Rerout\Models\Tag;
use Rerout\Models\UpdateTagInput;
use Rerout\Rerout;

/**
 * Tag management namespace. Reached via `Rerout::tags()`.
 *
 * Manages the project's tags (list, create, update, delete). The project is
 * resolved from the API key — there is no project id in the path.
 */
final class Tags
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /**
     * List the project's tags along with each tag's live link count. This is
     * the only place `linkCount` is returned — `create`/`update` omit it.
     */
    public function list(): ListTagsResult
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/projects/me/tags',
        );

        return ListTagsResult::fromArray($response);
    }

    /** Create a tag. The server validates `color` and defaults it to `teal`. */
    public function create(CreateTagInput $input): Tag
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'POST',
            path: '/v1/projects/me/tags',
            body: $input->toArray(),
        );

        return Tag::fromArray($response);
    }

    /**
     * Patch a tag. Only fields set on `$input` are sent. Throws
     * `ReroutException` with code `bad_request` when called with an empty
     * input — the API rejects no-op PATCH bodies.
     */
    public function update(string $tagId, UpdateTagInput $input): Tag
    {
        if ($input->isEmpty()) {
            throw new ReroutException(
                errorCode: 'bad_request',
                message: 'UpdateTagInput has no fields to send.',
                status: 0,
                path: '/v1/projects/me/tags/' . $tagId,
            );
        }

        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'PATCH',
            path: '/v1/projects/me/tags/' . rawurlencode($tagId),
            body: $input->toArray(),
        );

        return Tag::fromArray($response);
    }

    /**
     * Delete a tag. Also drops the tag's link assignments server-side.
     *
     * @return bool Whether the server confirmed deletion.
     */
    public function delete(string $tagId): bool
    {
        $response = $this->client->request(
            method: 'DELETE',
            path: '/v1/projects/me/tags/' . rawurlencode($tagId),
        );

        if (is_array($response) && array_key_exists('deleted', $response)) {
            $deleted = $response['deleted'];

            return is_bool($deleted) ? $deleted : true;
        }

        return true;
    }
}
