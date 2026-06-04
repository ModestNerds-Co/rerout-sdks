//! Integration tests for Smart Links — the new `Link` response fields, the
//! `CreateLinkInput` / `UpdateLinkInput` Smart Links inputs, conversion
//! recording, and batch link creation. Mocked HTTP throughout (no network).

use rerout::{
    BatchLinkInput, CreateAbVariantInput, CreateLinkInput, RecordConversionInput, Rerout,
    ReroutError, RoutingRule, UpdateLinkInput,
};
use wiremock::matchers::{body_json_string, method, path};
use wiremock::{Mock, MockServer, ResponseTemplate};

async fn client_for(server: &MockServer) -> Rerout {
    Rerout::builder("rrk_test")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap()
}

/// A link envelope carrying populated Smart Links fields.
fn smart_link_json() -> serde_json::Value {
    serde_json::json!({
        "code": "q4",
        "short_url": "https://rerout.co/q4",
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
        "tags": [],
        "password_protected": true,
        "max_clicks": 1000,
        "click_count": 42,
        "track_conversions": true,
        "routing_rules": [
            {"condition_type": "country", "condition_op": "is", "condition_value": "US", "target_url": "https://example.com/us"},
            {"condition_type": "device", "condition_op": "in", "condition_value": "ios,android", "target_url": "https://example.com/app"}
        ],
        "ab_variants": [
            {"id": 1, "target_url": "https://example.com/a", "weight": 60},
            {"id": 2, "target_url": "https://example.com/b", "weight": 40}
        ],
        "created_at": 1_700_000_000,
        "updated_at": 1_700_000_100,
    })
}

// ─── Link response: Smart Links fields ──────────────────────────────────────

#[tokio::test]
async fn get_decodes_smart_links_fields() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/q4"))
        .respond_with(ResponseTemplate::new(200).set_body_json(smart_link_json()))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let link = client.links().get("q4").await.expect("get succeeds");
    assert!(link.password_protected);
    assert_eq!(link.max_clicks, Some(1000));
    assert_eq!(link.click_count, 42);
    assert!(link.track_conversions);
    assert_eq!(link.routing_rules.len(), 2);
    assert_eq!(link.routing_rules[0].condition_type, "country");
    assert_eq!(link.routing_rules[0].condition_op, "is");
    assert_eq!(link.routing_rules[0].condition_value, "US");
    assert_eq!(link.routing_rules[0].target_url, "https://example.com/us");
    assert_eq!(link.routing_rules[1].condition_op, "in");
    assert_eq!(link.ab_variants.len(), 2);
    assert_eq!(link.ab_variants[0].id, 1);
    assert_eq!(link.ab_variants[0].weight, 60);
    assert_eq!(link.ab_variants[1].target_url, "https://example.com/b");
}

#[tokio::test]
async fn get_decodes_null_max_clicks() {
    let server = MockServer::start().await;
    let mut body = smart_link_json();
    body["max_clicks"] = serde_json::Value::Null;
    body["password_protected"] = serde_json::json!(false);
    Mock::given(method("GET"))
        .and(path("/v1/links/q4"))
        .respond_with(ResponseTemplate::new(200).set_body_json(body))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let link = client.links().get("q4").await.expect("get succeeds");
    assert_eq!(link.max_clicks, None);
    assert!(!link.password_protected);
}

// ─── CreateLinkInput: Smart Links serialization ─────────────────────────────

#[test]
fn create_input_serializes_smart_links_fields() {
    let input = CreateLinkInput::new("https://example.com")
        .with_password("hunter2")
        .with_max_clicks(500)
        .with_track_conversions(true)
        .with_routing_rules(vec![RoutingRule {
            condition_type: "country".to_string(),
            condition_op: "is".to_string(),
            condition_value: "US".to_string(),
            target_url: "https://example.com/us".to_string(),
        }])
        .with_ab_variants(vec![
            CreateAbVariantInput::new("https://example.com/a").with_weight(70),
            CreateAbVariantInput::new("https://example.com/b"),
        ]);
    let value = serde_json::to_value(&input).unwrap();
    assert_eq!(value["password"], "hunter2");
    assert_eq!(value["max_clicks"], 500);
    assert_eq!(value["track_conversions"], true);
    assert_eq!(value["routing_rules"][0]["condition_type"], "country");
    assert_eq!(value["routing_rules"][0]["target_url"], "https://example.com/us");
    assert_eq!(value["ab_variants"][0]["weight"], 70);
    // Variant without an explicit weight omits the field entirely.
    assert!(value["ab_variants"][1].get("weight").is_none());
}

#[test]
fn create_input_omits_smart_links_when_unset() {
    let input = CreateLinkInput::new("https://example.com");
    let value = serde_json::to_value(&input).unwrap();
    for key in ["password", "max_clicks", "track_conversions"] {
        assert!(value.get(key).is_none(), "expected {key} omitted");
    }
    // Empty vecs are skipped.
    assert!(value.get("routing_rules").is_none());
    assert!(value.get("ab_variants").is_none());
}

// ─── UpdateLinkInput: set / clear / full-replace serialization ──────────────

#[test]
fn update_input_set_and_clear_smart_links() {
    let input = UpdateLinkInput::new()
        .set_password("newpass")
        .clear_max_clicks()
        .set_track_conversions(false)
        .set_routing_rules(vec![RoutingRule {
            condition_type: "device".to_string(),
            condition_op: "is_not".to_string(),
            condition_value: "ios".to_string(),
            target_url: "https://example.com/web".to_string(),
        }])
        .set_ab_variants(vec![]);
    let value = serde_json::to_value(&input).unwrap();
    assert_eq!(value["password"], "newpass");
    assert!(value["max_clicks"].is_null());
    assert_eq!(value["track_conversions"], false);
    assert_eq!(value["routing_rules"][0]["condition_op"], "is_not");
    // A Some(empty vec) serializes as an explicit empty array (clears all).
    assert_eq!(value["ab_variants"], serde_json::json!([]));
}

#[test]
fn update_input_clear_password_serializes_null() {
    let input = UpdateLinkInput::new().clear_password();
    let value = serde_json::to_value(&input).unwrap();
    assert!(value["password"].is_null());
}

#[test]
fn update_input_leaves_smart_links_alone_when_unset() {
    let input = UpdateLinkInput::new().set_target_url("https://example.com/new");
    let value = serde_json::to_value(&input).unwrap();
    for key in [
        "password",
        "max_clicks",
        "track_conversions",
        "routing_rules",
        "ab_variants",
    ] {
        assert!(value.get(key).is_none(), "expected {key} omitted");
    }
}

#[test]
fn update_input_is_empty_tracks_smart_links() {
    assert!(UpdateLinkInput::new().is_empty());
    assert!(!UpdateLinkInput::new().set_password("x").is_empty());
    assert!(!UpdateLinkInput::new().clear_password().is_empty());
    assert!(!UpdateLinkInput::new().set_max_clicks(1).is_empty());
    assert!(!UpdateLinkInput::new().clear_max_clicks().is_empty());
    assert!(!UpdateLinkInput::new().set_track_conversions(true).is_empty());
    assert!(!UpdateLinkInput::new().set_routing_rules(vec![]).is_empty());
    assert!(!UpdateLinkInput::new().set_ab_variants(vec![]).is_empty());
}

// ─── conversions.record ─────────────────────────────────────────────────────

#[tokio::test]
async fn record_posts_conversion_and_returns_result() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "click_id": "clk_123",
        "event_name": "purchase",
        "value_cents": 4999,
        "currency": "USD",
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/conversions"))
        .and(body_json_string(expected))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(serde_json::json!({ "recorded": true, "duplicate": false })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .conversions()
        .record(
            &RecordConversionInput::new("clk_123", "purchase")
                .with_value_cents(4999)
                .with_currency("USD"),
        )
        .await
        .expect("record succeeds");
    assert!(result.recorded);
    assert!(!result.duplicate);
}

#[tokio::test]
async fn record_omits_optional_fields() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "click_id": "clk_1",
        "event_name": "signup",
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/conversions"))
        .and(body_json_string(expected))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(serde_json::json!({ "recorded": true, "duplicate": false })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .conversions()
        .record(&RecordConversionInput::new("clk_1", "signup"))
        .await
        .expect("record succeeds");
    assert!(result.recorded);
}

#[tokio::test]
async fn record_reports_duplicate() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/conversions"))
        .respond_with(
            ResponseTemplate::new(200)
                .set_body_json(serde_json::json!({ "recorded": false, "duplicate": true })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .conversions()
        .record(&RecordConversionInput::new("clk_1", "purchase"))
        .await
        .expect("record succeeds");
    assert!(!result.recorded);
    assert!(result.duplicate);
}

#[tokio::test]
async fn record_surfaces_server_errors() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/conversions"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "click_not_found",
            "message": "no such click.",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .conversions()
        .record(&RecordConversionInput::new("clk_missing", "purchase"))
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "click_not_found");
    assert_eq!(err.status(), 404);
}

// ─── links.create_batch ─────────────────────────────────────────────────────

#[tokio::test]
async fn create_batch_posts_links_and_returns_results() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "links": [
            {"target_url": "https://example.com/1"},
            {"target_url": "https://example.com/2", "code": "def", "domain_hostname": "go.brand.com"},
        ]
    })
    .to_string();
    Mock::given(method("POST"))
        .and(path("/v1/links/batch"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "created": 1,
            "total": 2,
            "results": [
                {"index": 0, "ok": true, "code": "abc", "error": null},
                {"index": 1, "ok": false, "code": null, "error": "duplicate code"},
            ],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client
        .links()
        .create_batch(&[
            BatchLinkInput::new("https://example.com/1"),
            BatchLinkInput::new("https://example.com/2")
                .with_code("def")
                .with_domain_hostname("go.brand.com"),
        ])
        .await
        .expect("create_batch succeeds");
    assert_eq!(result.created, 1);
    assert_eq!(result.total, 2);
    assert_eq!(result.results.len(), 2);
    assert!(result.results[0].ok);
    assert_eq!(result.results[0].code.as_deref(), Some("abc"));
    assert!(!result.results[1].ok);
    assert_eq!(result.results[1].index, 1);
    assert_eq!(result.results[1].error.as_deref(), Some("duplicate code"));
}

#[tokio::test]
async fn create_batch_rejects_empty_input_without_network() {
    let server = MockServer::start().await;
    // No mock mounted — any request would 404 (wiremock default), so a network
    // hit would surface as an error other than the config one we assert below.
    let client = client_for(&server).await;
    let err = client
        .links()
        .create_batch(&[])
        .await
        .expect_err("empty batch errors client-side");
    match err {
        ReroutError::Config { code, .. } => assert_eq!(code, "bad_request"),
        other => panic!("expected config error, got {other:?}"),
    }
}

#[tokio::test]
async fn create_batch_surfaces_server_errors() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/links/batch"))
        .respond_with(ResponseTemplate::new(413).set_body_json(serde_json::json!({
            "code": "batch_too_large",
            "message": "too many links.",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .links()
        .create_batch(&[BatchLinkInput::new("https://example.com/1")])
        .await
        .expect_err("413 errors");
    assert_eq!(err.code(), "batch_too_large");
    assert_eq!(err.status(), 413);
}
