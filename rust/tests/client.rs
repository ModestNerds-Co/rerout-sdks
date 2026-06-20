//! Integration tests for the [`Rerout`] client: construction, transport,
//! auth headers, query params, error parsing, and synthetic codes.

use rerout::{CreateLinkInput, ListLinksParams, Rerout, ReroutError};
use wiremock::matchers::{body_json_string, header, header_exists, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

/// A minimal `Link` JSON envelope the server returns for create/get/update.
fn link_json(code: &str) -> serde_json::Value {
    serde_json::json!({
        "code": code,
        "short_url": format!("https://rerout.co/{code}"),
        "domain_hostname": null,
        "target_url": "https://example.com/sale",
        "project_id": "prj_123",
        "expires_at": null,
        "is_active": true,
        "seo_title": null,
        "seo_description": null,
        "seo_image_url": null,
        "seo_canonical_url": null,
        "seo_noindex": false,
        "seo_updated_at": null,
        "created_at": 1_700_000_000,
        "updated_at": 1_700_000_000,
    })
}

// ─── Constructor ─────────────────────────────────────────────────────────────

#[test]
fn new_rejects_blank_api_key() {
    let err = Rerout::new("").expect_err("blank key must fail");
    assert!(matches!(err, ReroutError::Config { .. }));
    assert_eq!(err.code(), "missing_api_key");
    assert_eq!(err.status(), 0);
}

#[test]
fn new_rejects_whitespace_only_api_key() {
    let err = Rerout::new("   ").expect_err("whitespace key must fail");
    assert_eq!(err.code(), "missing_api_key");
}

#[test]
fn builder_accepts_a_valid_api_key() {
    let client = Rerout::new("rrk_live_abc").expect("valid key builds");
    assert_eq!(client.base_url(), "https://api.rerout.co");
}

#[test]
fn base_url_defaults_to_production() {
    let client = Rerout::builder("rrk_x").build().unwrap();
    assert_eq!(client.base_url(), rerout::DEFAULT_BASE_URL);
}

#[test]
fn base_url_trims_trailing_slashes() {
    let client = Rerout::builder("rrk_x")
        .base_url("https://api.staging.rerout.co///")
        .unwrap()
        .build()
        .unwrap();
    assert_eq!(client.base_url(), "https://api.staging.rerout.co");
}

#[test]
fn base_url_rejects_empty_string() {
    let err = Rerout::builder("rrk_x")
        .base_url("   /// ")
        .expect_err("blank base_url must fail");
    assert_eq!(err.code(), "invalid_base_url");
}

#[test]
fn base_url_rejects_unparseable_url() {
    let err = Rerout::builder("rrk_x")
        .base_url("not a url")
        .expect_err("garbage base_url must fail");
    assert_eq!(err.code(), "invalid_base_url");
}

#[test]
fn namespaces_are_present() {
    let client = Rerout::new("rrk_x").unwrap();
    // The namespace accessors return; reaching here means they all exist.
    let _ = client.links();
    let _ = client.project();
    let _ = client.qr();
    let _ = client.webhooks();
    let _ = client.conversions();
    let _ = client.tags();
}

#[test]
fn client_is_cloneable() {
    let client = Rerout::new("rrk_x").unwrap();
    let cloned = client.clone();
    assert_eq!(client.base_url(), cloned.base_url());
}

// ─── Request transport ──────────────────────────────────────────────────────

#[tokio::test]
async fn sends_bearer_authorization_header() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/abc"))
        .and(header("authorization", "Bearer rrk_secret_token"))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_secret_token")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();

    let link = client.links().get("abc").await.expect("call succeeds");
    assert_eq!(link.code, "abc");
}

#[tokio::test]
async fn sends_accept_application_json_header() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/abc"))
        .and(header("accept", "application/json"))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client.links().get("abc").await.expect("call succeeds");
}

#[tokio::test]
async fn sends_content_type_only_when_a_body_is_present() {
    let server = MockServer::start().await;
    // POST carries a body — content-type must be present.
    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .and(header("content-type", "application/json"))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("new1")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client
        .links()
        .create(&CreateLinkInput::new("https://example.com/sale"))
        .await
        .expect("create succeeds");
}

#[tokio::test]
async fn omits_content_type_on_bodyless_get() {
    let server = MockServer::start().await;
    // A GET has no body; assert content-type is absent by failing the mock if present.
    Mock::given(method("GET"))
        .and(path("/v1/links/abc"))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client.links().get("abc").await.expect("get succeeds");

    let requests = server.received_requests().await.unwrap();
    let get = requests
        .iter()
        .find(|r| r.url.path() == "/v1/links/abc")
        .unwrap();
    assert!(
        get.headers.get("content-type").is_none(),
        "GET request must not carry a content-type header",
    );
}

#[tokio::test]
async fn serializes_the_json_request_body() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "target_url": "https://example.com/promo",
        "code": "promo",
    })
    .to_string();

    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("promo")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let input = CreateLinkInput::new("https://example.com/promo").with_code("promo");
    let link = client
        .links()
        .create(&input)
        .await
        .expect("create succeeds");
    assert_eq!(link.code, "promo");
}

#[tokio::test]
async fn omits_optional_fields_from_the_create_body() {
    let server = MockServer::start().await;
    // Only `target_url` should appear — no nulls for the unset SEO fields.
    let expected = serde_json::json!({ "target_url": "https://example.com/x" }).to_string();
    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("x1")))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client
        .links()
        .create(&CreateLinkInput::new("https://example.com/x"))
        .await
        .expect("create succeeds");
}

#[tokio::test]
async fn forwards_cursor_and_limit_query_params() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links"))
        .and(query_param("cursor", "42"))
        .and(query_param("limit", "10"))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(serde_json::json!({ "links": [], "next_cursor": null })),
        )
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let params = ListLinksParams {
        cursor: Some(42),
        limit: Some(10),
    };
    client.links().list(params).await.expect("list succeeds");
}

#[tokio::test]
async fn omits_query_params_when_list_params_are_empty() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links"))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(serde_json::json!({ "links": [], "next_cursor": null })),
        )
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client
        .links()
        .list(ListLinksParams::default())
        .await
        .expect("list succeeds");

    let requests = server.received_requests().await.unwrap();
    let listing = requests
        .iter()
        .find(|r| r.url.path() == "/v1/links")
        .unwrap();
    assert_eq!(listing.url.query(), None, "no query string expected");
}

#[tokio::test]
async fn forwards_days_query_param_on_stats() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/stats"))
        .and(query_param("days", "7"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "days": 7,
            "total_clicks": 0,
            "qr_scans": 0,
            "daily": [],
            "countries": [],
            "referrers": [],
            "devices": [],
            "browsers": [],
            "top_codes": [],
        })))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let stats = client.project().stats(7).await.expect("stats succeeds");
    assert_eq!(stats.days, 7);
}

// ─── Error parsing ──────────────────────────────────────────────────────────

#[tokio::test]
async fn preserves_server_error_code_and_message() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(400).set_body_json(serde_json::json!({
            "code": "bad_target_url",
            "message": "target_url must use https.",
        })))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let err = client
        .links()
        .create(&CreateLinkInput::new("http://insecure.example"))
        .await
        .expect_err("400 must surface as an error");

    assert_eq!(err.code(), "bad_target_url");
    assert_eq!(err.status(), 400);
    assert_eq!(err.message(), "target_url must use https.");
    assert!(matches!(err, ReroutError::Api { .. }));
}

#[tokio::test]
async fn preserves_error_path_and_timestamp_details() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/missing"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "no such link",
            "path": "/v1/links/missing",
            "timestamp": "2026-05-20T12:00:00Z",
            "details": { "field": "code" },
        })))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let err = client
        .links()
        .get("missing")
        .await
        .expect_err("404 must surface as an error");

    assert_eq!(err.path(), Some("/v1/links/missing"));
    assert_eq!(err.timestamp(), Some("2026-05-20T12:00:00Z"));
    assert_eq!(
        err.details()
            .and_then(|d| d.get("field"))
            .and_then(|v| v.as_str()),
        Some("code"),
    );
}

#[tokio::test]
async fn synthetic_code_for_401_with_no_body() {
    assert_synthetic_code(401, "unauthorized").await;
}

#[tokio::test]
async fn synthetic_code_for_403_with_no_body() {
    assert_synthetic_code(403, "forbidden").await;
}

#[tokio::test]
async fn synthetic_code_for_404_with_no_body() {
    assert_synthetic_code(404, "not_found").await;
}

#[tokio::test]
async fn synthetic_code_for_429_with_no_body() {
    let err = error_for_status(429).await;
    assert_eq!(err.code(), "rate_limited");
    assert!(err.is_rate_limited());
    assert!(!err.is_server_error());
}

#[tokio::test]
async fn synthetic_code_for_500_with_no_body() {
    let err = error_for_status(500).await;
    assert_eq!(err.code(), "server_error");
    assert!(err.is_server_error());
    assert!(!err.is_rate_limited());
}

#[tokio::test]
async fn synthetic_code_for_503_with_no_body() {
    let err = error_for_status(503).await;
    assert_eq!(err.code(), "server_error");
    assert!(err.is_server_error());
}

#[tokio::test]
async fn synthetic_code_for_generic_4xx_with_no_body() {
    assert_synthetic_code(418, "client_error").await;
}

#[tokio::test]
async fn synthetic_code_for_non_json_error_body() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/x"))
        .respond_with(ResponseTemplate::new(500).set_body_string("<html>down</html>"))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let err = client.links().get("x").await.expect_err("500 errors");
    assert_eq!(err.code(), "server_error");
    assert_eq!(err.status(), 500);
}

#[tokio::test]
async fn network_failure_maps_to_network_error() {
    // Point at a port nothing listens on; the connection is refused.
    let client = Rerout::builder("rrk_x")
        .base_url("http://127.0.0.1:1")
        .unwrap()
        .build()
        .unwrap();
    let err = client
        .links()
        .get("x")
        .await
        .expect_err("connection refused");
    assert_eq!(err.code(), "network_error");
    assert_eq!(err.status(), 0);
    assert!(matches!(err, ReroutError::Network { .. }));
}

#[tokio::test]
async fn timeout_maps_to_timeout_error() {
    use std::time::Duration;

    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/slow"))
        .respond_with(
            ResponseTemplate::new(200)
                .set_delay(Duration::from_secs(5))
                .set_body_json(link_json("slow")),
        )
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .timeout(Duration::from_millis(80))
        .build()
        .unwrap();
    let err = client.links().get("slow").await.expect_err("must time out");
    assert_eq!(err.code(), "timeout");
    assert_eq!(err.status(), 0);
    assert!(matches!(err, ReroutError::Timeout { .. }));
}

#[tokio::test]
async fn non_json_2xx_body_maps_to_decode_error() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/weird"))
        .respond_with(ResponseTemplate::new(200).set_body_string("definitely not json"))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let err = client
        .links()
        .get("weird")
        .await
        .expect_err("non-JSON 2xx body must fail");
    assert!(matches!(err, ReroutError::Decode { .. }));
    assert_eq!(err.code(), "decode_error");
}

#[tokio::test]
async fn empty_2xx_body_maps_to_unexpected_response() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/empty"))
        .respond_with(ResponseTemplate::new(200).set_body_string(""))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    let err = client
        .links()
        .get("empty")
        .await
        .expect_err("empty 2xx body must fail");
    assert!(matches!(err, ReroutError::Unexpected { .. }));
    assert_eq!(err.code(), "unexpected_response");
}

#[tokio::test]
async fn user_agent_override_is_sent() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me"))
        .and(header_exists("user-agent"))
        .and(header("user-agent", "my-app/9.9"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id": "prj_1",
            "name": "Acme",
            "slug": "acme",
        })))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .user_agent("my-app/9.9")
        .build()
        .unwrap();
    client.project().me().await.expect("me succeeds");
}

// ─── Helpers ────────────────────────────────────────────────────────────────

async fn error_for_status(status: u16) -> ReroutError {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/x"))
        .respond_with(ResponseTemplate::new(status))
        .mount(&server)
        .await;

    let client = Rerout::builder("rrk_x")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap();
    client
        .links()
        .get("x")
        .await
        .expect_err("non-2xx status must surface as an error")
}

async fn assert_synthetic_code(status: u16, expected_code: &str) {
    let err = error_for_status(status).await;
    assert_eq!(err.code(), expected_code);
    assert_eq!(err.status(), status);
}
