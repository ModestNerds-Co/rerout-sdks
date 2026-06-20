//! Integration tests for the `tags` management namespace — list, create,
//! update, and delete against a mocked HTTP server (no network). Mirrors the
//! TypeScript golden reference (`typescript/test/tags.test.ts`).

use rerout::{CreateTagInput, Rerout, UpdateTagInput};
use wiremock::matchers::{body_json_string, method, path};
use wiremock::{Mock, MockServer, ResponseTemplate};

async fn client_for(server: &MockServer) -> Rerout {
    Rerout::builder("rrk_test")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap()
}

// ─── tags.list ────────────────────────────────────────────────────────────────

#[tokio::test]
async fn list_gets_tags_and_returns_link_counts() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/tags"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "tags": [
                { "id": "tag_abc123", "name": "Spring 2026", "color": "teal", "link_count": 4 }
            ],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client.tags().list().await.expect("list succeeds");
    assert_eq!(result.tags.len(), 1);
    let summary = &result.tags[0];
    assert_eq!(summary.tag.id, "tag_abc123");
    assert_eq!(summary.tag.name, "Spring 2026");
    assert_eq!(summary.tag.color, "teal");
    assert_eq!(summary.link_count, 4);
}

#[tokio::test]
async fn list_defaults_link_count_when_missing() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/tags"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "tags": [
                { "id": "tag_x", "name": "No count", "color": "red" }
            ],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client.tags().list().await.expect("list succeeds");
    assert_eq!(result.tags[0].link_count, 0);
}

#[tokio::test]
async fn list_surfaces_unauthorized_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/tags"))
        .respond_with(ResponseTemplate::new(401).set_body_json(serde_json::json!({
            "code": "unauthorized",
            "message": "bad api key",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client.tags().list().await.expect_err("401 errors");
    assert_eq!(err.code(), "unauthorized");
    assert_eq!(err.status(), 401);
}

// ─── tags.create ──────────────────────────────────────────────────────────────

#[tokio::test]
async fn create_posts_the_name_and_color() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "name": "Spring 2026",
        "color": "teal",
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/tags"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(201).set_body_json(serde_json::json!({
            "id": "tag_abc123",
            "name": "Spring 2026",
            "color": "teal",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let tag = client
        .tags()
        .create(&CreateTagInput::new("Spring 2026").with_color("teal"))
        .await
        .expect("create succeeds");
    assert_eq!(tag.id, "tag_abc123");
    assert_eq!(tag.name, "Spring 2026");
    assert_eq!(tag.color, "teal");
}

#[tokio::test]
async fn create_omits_color_when_not_set() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({ "name": "No color" }).to_string();
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/tags"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(201).set_body_json(serde_json::json!({
            "id": "tag_nc",
            "name": "No color",
            "color": "teal",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let tag = client
        .tags()
        .create(&CreateTagInput::new("No color"))
        .await
        .expect("create succeeds");
    assert_eq!(tag.color, "teal");
}

#[tokio::test]
async fn create_surfaces_server_validation_errors() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/tags"))
        .respond_with(ResponseTemplate::new(400).set_body_json(serde_json::json!({
            "code": "bad_tag_color",
            "message": "color must be one of the palette values.",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .tags()
        .create(&CreateTagInput::new("Bad").with_color("not-a-color"))
        .await
        .expect_err("400 errors");
    assert_eq!(err.code(), "bad_tag_color");
    assert_eq!(err.status(), 400);
}

// ─── tags.update ──────────────────────────────────────────────────────────────

#[tokio::test]
async fn update_patches_the_tag_by_id() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({ "color": "red" }).to_string();
    Mock::given(method("PATCH"))
        .and(path("/v1/projects/me/tags/tag_abc123"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id": "tag_abc123",
            "name": "Spring 2026",
            "color": "red",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let tag = client
        .tags()
        .update("tag_abc123", &UpdateTagInput::new().with_color("red"))
        .await
        .expect("update succeeds");
    assert_eq!(tag.color, "red");
}

#[tokio::test]
async fn update_forwards_only_the_provided_fields() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({ "name": "Renamed" }).to_string();
    Mock::given(method("PATCH"))
        .and(path("/v1/projects/me/tags/tag_abc123"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id": "tag_abc123",
            "name": "Renamed",
            "color": "teal",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let tag = client
        .tags()
        .update("tag_abc123", &UpdateTagInput::new().with_name("Renamed"))
        .await
        .expect("update succeeds");
    assert_eq!(tag.name, "Renamed");
}

#[tokio::test]
async fn update_encodes_the_tag_id() {
    let server = MockServer::start().await;
    Mock::given(method("PATCH"))
        .and(path("/v1/projects/me/tags/tag_a%2Fb"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id": "tag_a/b",
            "name": "Slashed",
            "color": "teal",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let tag = client
        .tags()
        .update("tag_a/b", &UpdateTagInput::new().with_name("Slashed"))
        .await
        .expect("update succeeds against the encoded path");
    assert_eq!(tag.name, "Slashed");
}

// ─── tags.delete ──────────────────────────────────────────────────────────────

#[tokio::test]
async fn delete_sends_delete_and_returns_the_result() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/projects/me/tags/tag_abc123"))
        .respond_with(
            ResponseTemplate::new(200).set_body_json(serde_json::json!({ "deleted": true })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .tags()
        .delete("tag_abc123")
        .await
        .expect("delete succeeds");
    assert!(result.deleted);
}

#[tokio::test]
async fn delete_surfaces_not_found() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/projects/me/tags/tag_ghost"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "gone already",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .tags()
        .delete("tag_ghost")
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
}
