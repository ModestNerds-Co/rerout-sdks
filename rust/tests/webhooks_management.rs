//! Integration tests for the `webhooks` management namespace — create, list,
//! and delete against a mocked HTTP server (no network). Mirrors the depth of
//! the `links` tests and the TypeScript golden reference.

use rerout::{CreateWebhookInput, Rerout, WebhookPayloadFormat};
use wiremock::matchers::{body_json_string, method, path};
use wiremock::{Mock, MockServer, ResponseTemplate};

/// A webhook endpoint envelope matching `WebhookEndpointResponse`.
fn webhook_json(id: &str) -> serde_json::Value {
    serde_json::json!({
        "id": id,
        "project_id": "prj_test",
        "name": "Order events",
        "url": "https://example.com/hooks/rerout",
        "events": ["link.created", "link.clicked"],
        "is_active": true,
        "payload_format": "json",
        "created_at": 1_700_000_000_i64,
        "updated_at": 1_700_000_000_i64,
        "last_delivery_at": null,
        "last_success_at": null,
        "last_failure_at": null,
    })
}

async fn client_for(server: &MockServer) -> Rerout {
    Rerout::builder("rrk_test")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap()
}

// ─── webhooks.create ──────────────────────────────────────────────────────────

#[tokio::test]
async fn create_posts_and_returns_the_signing_secret() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "name": "Order events",
        "url": "https://example.com/hooks/rerout",
        "events": ["link.created", "link.clicked"],
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/webhooks"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(201).set_body_json(serde_json::json!({
            "endpoint": webhook_json("wh_abc123"),
            "signing_secret": "whsec_supersecret",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .webhooks()
        .create(&CreateWebhookInput::new(
            "Order events",
            "https://example.com/hooks/rerout",
            ["link.created", "link.clicked"],
        ))
        .await
        .expect("create succeeds");
    assert_eq!(result.signing_secret, "whsec_supersecret");
    assert_eq!(result.endpoint.id, "wh_abc123");
    assert_eq!(result.endpoint.events, vec!["link.created", "link.clicked"]);
    assert!(result.endpoint.is_active);
}

#[tokio::test]
async fn create_forwards_is_active_and_payload_format_when_provided() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "name": "Slack",
        "url": "https://hooks.slack.com/services/T/B/x",
        "events": ["link.created"],
        "is_active": false,
        "payload_format": "slack",
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/webhooks"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(201).set_body_json(serde_json::json!({
            "endpoint": {
                "id": "wh_slack",
                "project_id": "prj_test",
                "name": "Slack",
                "url": "https://hooks.slack.com/services/T/B/x",
                "events": ["link.created"],
                "is_active": false,
                "payload_format": "slack",
                "created_at": 1_700_000_000_i64,
                "updated_at": 1_700_000_000_i64,
                "last_delivery_at": null,
                "last_success_at": null,
                "last_failure_at": null,
            },
            "signing_secret": "whsec_x",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = CreateWebhookInput::new(
        "Slack",
        "https://hooks.slack.com/services/T/B/x",
        ["link.created"],
    )
    .with_is_active(false)
    .with_payload_format(WebhookPayloadFormat::Slack);
    let result = client
        .webhooks()
        .create(&input)
        .await
        .expect("create succeeds");
    assert_eq!(result.endpoint.payload_format, "slack");
    assert!(!result.endpoint.is_active);
}

#[tokio::test]
async fn create_surfaces_server_validation_errors() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/projects/me/webhooks"))
        .respond_with(ResponseTemplate::new(400).set_body_json(serde_json::json!({
            "code": "bad_webhook_url",
            "message": "url must be a public https URL.",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .webhooks()
        .create(&CreateWebhookInput::new(
            "Bad",
            "http://nope",
            ["link.created"],
        ))
        .await
        .expect_err("400 errors");
    assert_eq!(err.code(), "bad_webhook_url");
    assert_eq!(err.status(), 400);
}

// ─── webhooks.list ────────────────────────────────────────────────────────────

#[tokio::test]
async fn list_returns_endpoints_and_event_types() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/webhooks"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "endpoints": [webhook_json("wh_abc123")],
            "event_types": ["link.created", "link.clicked", "domain.verified"],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client.webhooks().list().await.expect("list succeeds");
    assert_eq!(result.endpoints.len(), 1);
    assert_eq!(result.endpoints[0].url, "https://example.com/hooks/rerout");
    assert!(result.event_types.contains(&"domain.verified".to_string()));
}

#[tokio::test]
async fn list_surfaces_unauthorized_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/webhooks"))
        .respond_with(ResponseTemplate::new(401).set_body_json(serde_json::json!({
            "code": "unauthorized",
            "message": "bad api key",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client.webhooks().list().await.expect_err("401 errors");
    assert_eq!(err.code(), "unauthorized");
    assert_eq!(err.status(), 401);
}

// ─── webhooks.delete ──────────────────────────────────────────────────────────

#[tokio::test]
async fn delete_reports_success() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/projects/me/webhooks/wh_abc123"))
        .respond_with(
            ResponseTemplate::new(200).set_body_json(serde_json::json!({ "deleted": true })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .webhooks()
        .delete("wh_abc123")
        .await
        .expect("delete succeeds");
    assert!(result.deleted);
}

#[tokio::test]
async fn delete_encodes_the_endpoint_id() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/projects/me/webhooks/wh_a%2Fb"))
        .respond_with(
            ResponseTemplate::new(200).set_body_json(serde_json::json!({ "deleted": true })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .webhooks()
        .delete("wh_a/b")
        .await
        .expect("delete succeeds against the encoded path");
    assert!(result.deleted);
}

#[tokio::test]
async fn delete_surfaces_not_found() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/projects/me/webhooks/wh_ghost"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "gone already",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .webhooks()
        .delete("wh_ghost")
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
}
