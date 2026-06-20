<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use Rerout\Exceptions\ReroutException;
use Rerout\Models\CreateTagInput;
use Rerout\Models\ListTagsResult;
use Rerout\Models\Tag;
use Rerout\Models\TagSummary;
use Rerout\Models\UpdateTagInput;
use Rerout\Resources\Tags;

/**
 * Coverage for the {@see Tags} namespace (`list` / `create` / `update` /
 * `delete`) against a mocked HTTP layer. Mirrors `typescript/test/tags.test.ts`.
 */
#[CoversClass(Tags::class)]
#[CoversClass(Tag::class)]
#[CoversClass(TagSummary::class)]
#[CoversClass(ListTagsResult::class)]
#[CoversClass(CreateTagInput::class)]
#[CoversClass(UpdateTagInput::class)]
final class TagsTest extends TestCase
{
    // ─── list ───────────────────────────────────────────────────────────────

    public function testListGetsTagsEndpointAndReturnsLinkCounts(): void
    {
        $this->queueJson([
            'tags' => [
                ['id' => 'tag_abc123', 'name' => 'Spring 2026', 'color' => 'teal', 'link_count' => 4],
            ],
        ]);

        $result = $this->client()->tags()->list();

        $request = $this->lastRequest();
        self::assertSame('GET', $request->getMethod());
        self::assertSame('/v1/projects/me/tags', $request->getUri()->getPath());

        self::assertInstanceOf(ListTagsResult::class, $result);
        self::assertCount(1, $result->tags);
        self::assertContainsOnlyInstancesOf(TagSummary::class, $result->tags);
        self::assertSame('tag_abc123', $result->tags[0]->id);
        self::assertSame('Spring 2026', $result->tags[0]->name);
        self::assertSame('teal', $result->tags[0]->color);
        self::assertSame(4, $result->tags[0]->linkCount);
    }

    public function testListHandlesEmptyResult(): void
    {
        $this->queueJson(['tags' => []]);

        $result = $this->client()->tags()->list();

        self::assertSame([], $result->tags);
    }

    public function testListErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'unauthorized', 'message' => 'bad key'], 401);

        $this->expectException(ReroutException::class);
        $this->client()->tags()->list();
    }

    // ─── create ─────────────────────────────────────────────────────────────

    public function testCreatePostsNameAndColor(): void
    {
        $this->queueJson(['id' => 'tag_abc123', 'name' => 'Spring 2026', 'color' => 'teal'], 201);

        $tag = $this->client()->tags()->create(new CreateTagInput(
            name: 'Spring 2026',
            color: 'teal',
        ));

        $request = $this->lastRequest();
        self::assertSame('POST', $request->getMethod());
        self::assertSame('/v1/projects/me/tags', $request->getUri()->getPath());

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $request->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['name' => 'Spring 2026', 'color' => 'teal'], $body);

        self::assertInstanceOf(Tag::class, $tag);
        self::assertSame('tag_abc123', $tag->id);
        self::assertSame('teal', $tag->color);
    }

    public function testCreateOmitsColorWhenNotProvided(): void
    {
        $this->queueJson(['id' => 'tag_x', 'name' => 'No colour', 'color' => 'teal'], 201);

        $this->client()->tags()->create(new CreateTagInput(name: 'No colour'));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['name' => 'No colour'], $body);
        self::assertArrayNotHasKey('color', $body);
    }

    public function testCreateErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'bad_request', 'message' => 'name required'], 400);

        $this->expectException(ReroutException::class);
        $this->client()->tags()->create(new CreateTagInput(name: ''));
    }

    // ─── update ─────────────────────────────────────────────────────────────

    public function testUpdatePatchesTagByIdAndForwardsColorOnly(): void
    {
        $this->queueJson(['id' => 'tag_abc123', 'name' => 'Spring 2026', 'color' => 'red']);

        $tag = $this->client()->tags()->update('tag_abc123', new UpdateTagInput(color: 'red'));

        $request = $this->lastRequest();
        self::assertSame('PATCH', $request->getMethod());
        self::assertSame('/v1/projects/me/tags/tag_abc123', $request->getUri()->getPath());

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $request->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['color' => 'red'], $body);

        self::assertSame('red', $tag->color);
    }

    public function testUpdateForwardsOnlyTheProvidedFields(): void
    {
        $this->queueJson(['id' => 'tag_abc123', 'name' => 'Renamed', 'color' => 'teal']);

        $tag = $this->client()->tags()->update('tag_abc123', new UpdateTagInput(name: 'Renamed'));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['name' => 'Renamed'], $body);
        self::assertArrayNotHasKey('color', $body);

        self::assertSame('Renamed', $tag->name);
    }

    public function testUpdateSendsBothFieldsWhenProvided(): void
    {
        $this->queueJson(['id' => 'tag_abc123', 'name' => 'Renamed', 'color' => 'red']);

        $this->client()->tags()->update('tag_abc123', new UpdateTagInput(name: 'Renamed', color: 'red'));

        /** @var array<string, mixed> $body */
        $body = json_decode((string) $this->lastRequest()->getBody(), true, 512, JSON_THROW_ON_ERROR);
        self::assertSame(['name' => 'Renamed', 'color' => 'red'], $body);
    }

    public function testUpdateWithEmptyInputThrowsBeforeHittingApi(): void
    {
        try {
            $this->client()->tags()->update('tag_abc123', new UpdateTagInput());
            self::fail('Expected ReroutException for empty UpdateTagInput.');
        } catch (ReroutException $e) {
            self::assertSame('bad_request', $e->errorCode);
        }

        self::assertCount(0, $this->transactions, 'No request should be sent for an empty update.');
    }

    public function testUpdateErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->tags()->update('tag_missing', new UpdateTagInput(name: 'X'));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    public function testDeleteSendsDeleteAndReturnsTrue(): void
    {
        $this->queueJson(['deleted' => true]);

        $deleted = $this->client()->tags()->delete('tag_abc123');

        $request = $this->lastRequest();
        self::assertSame('DELETE', $request->getMethod());
        self::assertSame('/v1/projects/me/tags/tag_abc123', $request->getUri()->getPath());
        self::assertTrue($deleted);
    }

    public function testDeleteReturnsFalseWhenServerReportsNotDeleted(): void
    {
        $this->queueJson(['deleted' => false]);

        self::assertFalse($this->client()->tags()->delete('tag_abc123'));
    }

    public function testDeleteReturnsTrueOnEmptyBody(): void
    {
        $this->queueRaw('', 204);

        self::assertTrue($this->client()->tags()->delete('tag_abc123'));
    }

    public function testDeleteErrorPathThrowsReroutException(): void
    {
        $this->queueJson(['code' => 'not_found', 'message' => 'gone'], 404);

        $this->expectException(ReroutException::class);
        $this->client()->tags()->delete('tag_missing');
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    /**
     * @return array<string, array{0: string, 1: string}>
     */
    public static function tagIdEncodingProvider(): array
    {
        return [
            'plain' => ['tag_abc123', '/v1/projects/me/tags/tag_abc123'],
            'space' => ['tag abc', '/v1/projects/me/tags/tag%20abc'],
            'plus' => ['a+b', '/v1/projects/me/tags/a%2Bb'],
            'unicode' => ['café', '/v1/projects/me/tags/caf%C3%A9'],
            'slash' => ['go/promo', '/v1/projects/me/tags/go%2Fpromo'],
        ];
    }

    #[DataProvider('tagIdEncodingProvider')]
    public function testDeleteEncodesTagIdInPath(string $tagId, string $expectedPath): void
    {
        $this->queueJson(['deleted' => true]);

        $this->client()->tags()->delete($tagId);

        self::assertSame($expectedPath, $this->lastRequest()->getUri()->getPath());
    }

    // ─── transport: auth header ─────────────────────────────────────────────

    public function testListSendsBearerAuthHeader(): void
    {
        $this->queueJson(['tags' => []]);

        $this->client('rrk_secret')->tags()->list();

        self::assertSame('Bearer rrk_secret', $this->lastRequest()->getHeaderLine('Authorization'));
    }
}
