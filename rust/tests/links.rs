//! Integration tests for the `links` and `project` namespaces — one success
//! path and one error path per method, plus URL-encoding edge cases.

use rerout::{CreateLinkInput, ListLinksParams, Rerout, ReroutError, UpdateLinkInput};
use wiremock::matchers::{body_json_string, method, path};
use wiremock::{Mock, MockServer, ResponseTemplate};

/// A link envelope with an empty `tags` array — matches the `POST /v1/links`
/// shape, where tags are not yet populated.
fn link_json(code: &str) -> serde_json::Value {
    link_json_with_tags(code, serde_json::json!([]))
}

/// A link envelope with a caller-supplied `tags` array. `GET`, `list`, and
/// `PATCH` responses populate tags.
fn link_json_with_tags(code: &str, tags: serde_json::Value) -> serde_json::Value {
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
        "updated_at": 1_700_000_100,
        "tags": tags,
    })
}

async fn client_for(server: &MockServer) -> Rerout {
    Rerout::builder("rrk_test")
        .base_url(server.uri())
        .unwrap()
        .build()
        .unwrap()
}

// ─── links.create ───────────────────────────────────────────────────────────

#[tokio::test]
async fn create_returns_the_new_link() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(201).set_body_json(link_json("q4")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let link = client
        .links()
        .create(&CreateLinkInput::new("https://example.com/sale"))
        .await
        .expect("create succeeds");
    assert_eq!(link.code, "q4");
    assert_eq!(link.short_url, "https://rerout.co/q4");
    assert!(link.is_active);
    assert!(link.tags.is_empty(), "create returns an empty tags array");
}

#[tokio::test]
async fn create_serializes_all_builder_fields() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "target_url": "https://example.com/sale",
        "domain_hostname": "go.brand.com",
        "code": "q4",
        "expires_at": 1_800_000_000_i64,
        "seo_title": "Q4 Sale",
        "seo_description": "Up to 50% off",
        "seo_image_url": "https://cdn.example.com/q4.png",
        "seo_canonical_url": "https://example.com/q4",
        "seo_noindex": true,
    })
    .to_string();

    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(201).set_body_json(link_json("q4")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = CreateLinkInput::new("https://example.com/sale")
        .with_domain_hostname("go.brand.com")
        .with_code("q4")
        .with_expires_at(1_800_000_000)
        .with_seo_title("Q4 Sale")
        .with_seo_description("Up to 50% off")
        .with_seo_image_url("https://cdn.example.com/q4.png")
        .with_seo_canonical_url("https://example.com/q4")
        .with_seo_noindex(true);
    client
        .links()
        .create(&input)
        .await
        .expect("create succeeds");
}

#[tokio::test]
async fn create_surfaces_server_validation_errors() {
    let server = MockServer::start().await;
    Mock::given(method("POST"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(400).set_body_json(serde_json::json!({
            "code": "bad_target_url",
            "message": "target_url must use https.",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .links()
        .create(&CreateLinkInput::new("http://nope"))
        .await
        .expect_err("400 errors");
    assert_eq!(err.code(), "bad_target_url");
}

// ─── links.list ─────────────────────────────────────────────────────────────

#[tokio::test]
async fn list_returns_a_page_of_links() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "links": [link_json("a"), link_json("b")],
            "next_cursor": 99,
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let page = client
        .links()
        .list(ListLinksParams::default())
        .await
        .expect("list succeeds");
    assert_eq!(page.links.len(), 2);
    assert_eq!(page.next_cursor, Some(99));
    assert!(page.has_more());
}

#[tokio::test]
async fn list_reports_last_page_with_null_cursor() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "links": [link_json("only")],
            "next_cursor": null,
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let page = client
        .links()
        .list(ListLinksParams::default())
        .await
        .expect("list succeeds");
    assert!(!page.has_more());
    assert_eq!(page.next_cursor, None);
}

#[tokio::test]
async fn list_surfaces_unauthorized_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links"))
        .respond_with(ResponseTemplate::new(401).set_body_json(serde_json::json!({
            "code": "unauthorized",
            "message": "bad api key",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .links()
        .list(ListLinksParams::default())
        .await
        .expect_err("401 errors");
    assert_eq!(err.code(), "unauthorized");
    assert_eq!(err.status(), 401);
}

// ─── links.get ──────────────────────────────────────────────────────────────

#[tokio::test]
async fn get_returns_a_single_link_with_tags() {
    let server = MockServer::start().await;
    let body = link_json_with_tags(
        "abc",
        serde_json::json!([
            { "id": "tag_1", "name": "campaign", "color": "#3b82f6" },
        ]),
    );
    Mock::given(method("GET"))
        .and(path("/v1/links/abc"))
        .respond_with(ResponseTemplate::new(200).set_body_json(body))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let link = client.links().get("abc").await.expect("get succeeds");
    assert_eq!(link.code, "abc");
    assert_eq!(link.tags.len(), 1);
    assert_eq!(link.tags[0].id, "tag_1");
    assert_eq!(link.tags[0].name, "campaign");
    assert_eq!(link.tags[0].color, "#3b82f6");
}

#[tokio::test]
async fn get_surfaces_not_found() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/ghost"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "no such link",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client.links().get("ghost").await.expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
}

// ─── links.update ───────────────────────────────────────────────────────────

#[tokio::test]
async fn update_patches_a_link() {
    let server = MockServer::start().await;
    Mock::given(method("PATCH"))
        .and(path("/v1/links/abc"))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = UpdateLinkInput::new().set_is_active(false);
    let link = client
        .links()
        .update("abc", &input)
        .await
        .expect("update succeeds");
    assert_eq!(link.code, "abc");
}

#[tokio::test]
async fn update_rejects_an_empty_payload_without_a_network_call() {
    let server = MockServer::start().await;
    // No mock mounted: if a request escaped, the server would 404.
    let client = client_for(&server).await;
    let err = client
        .links()
        .update("abc", &UpdateLinkInput::new())
        .await
        .expect_err("empty update must fail client-side");
    assert!(matches!(err, ReroutError::Config { .. }));
    assert_eq!(err.code(), "bad_request");
    assert_eq!(err.status(), 0);

    let requests = server.received_requests().await.unwrap();
    assert!(requests.is_empty(), "empty update must not hit the network");
}

#[tokio::test]
async fn update_clear_sends_explicit_null() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({ "expires_at": null }).to_string();
    Mock::given(method("PATCH"))
        .and(path("/v1/links/abc"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = UpdateLinkInput::new().clear_expires_at();
    client
        .links()
        .update("abc", &input)
        .await
        .expect("clear update succeeds");
}

#[tokio::test]
async fn update_set_sends_the_value() {
    let server = MockServer::start().await;
    let expected = serde_json::json!({
        "target_url": "https://example.com/new",
        "seo_title": "Fresh title",
    })
    .to_string();
    Mock::given(method("PATCH"))
        .and(path("/v1/links/abc"))
        .and(body_json_string(expected))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("abc")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = UpdateLinkInput::new()
        .set_target_url("https://example.com/new")
        .set_seo_title("Fresh title");
    client
        .links()
        .update("abc", &input)
        .await
        .expect("set update succeeds");
}

#[tokio::test]
async fn update_surfaces_server_errors() {
    let server = MockServer::start().await;
    Mock::given(method("PATCH"))
        .and(path("/v1/links/abc"))
        .respond_with(ResponseTemplate::new(409).set_body_json(serde_json::json!({
            "code": "conflict",
            "message": "code already taken",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let input = UpdateLinkInput::new().set_is_active(true);
    let err = client
        .links()
        .update("abc", &input)
        .await
        .expect_err("409 errors");
    assert_eq!(err.code(), "conflict");
    assert_eq!(err.status(), 409);
}

// ─── links.delete ───────────────────────────────────────────────────────────

#[tokio::test]
async fn delete_reports_success() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/links/abc"))
        .respond_with(
            ResponseTemplate::new(200).set_body_json(serde_json::json!({ "deleted": true })),
        )
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let result = client.links().delete("abc").await.expect("delete succeeds");
    assert!(result.deleted);
}

#[tokio::test]
async fn delete_surfaces_not_found() {
    let server = MockServer::start().await;
    Mock::given(method("DELETE"))
        .and(path("/v1/links/ghost"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "gone already",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .links()
        .delete("ghost")
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
}

// ─── links.stats ────────────────────────────────────────────────────────────

#[tokio::test]
async fn stats_returns_link_analytics() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/abc/stats"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "code": "abc",
            "days": 30,
            "total_clicks": 128,
            "qr_scans": 19,
            "countries": [{ "value": "ZA", "clicks": 80 }],
            "referrers": [{ "value": "twitter.com", "clicks": 40 }],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let stats = client
        .links()
        .stats("abc", 30)
        .await
        .expect("stats succeeds");
    assert_eq!(stats.total_clicks, 128);
    assert_eq!(stats.qr_scans, 19);
    assert_eq!(stats.countries[0].value, "ZA");
    assert_eq!(stats.referrers[0].clicks, 40);
}

#[tokio::test]
async fn stats_surfaces_not_found() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/ghost/stats"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "no such link",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client
        .links()
        .stats("ghost", 30)
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
}

// ─── project namespace ──────────────────────────────────────────────────────

#[tokio::test]
async fn project_stats_returns_aggregate_analytics() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/stats"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "days": 30,
            "total_clicks": 5000,
            "qr_scans": 700,
            "daily": [{ "day": 1_700_000_000_i64, "clicks": 12, "qr_scans": 3 }],
            "countries": [{ "value": "US", "clicks": 2000 }],
            "referrers": [],
            "devices": [{ "value": "mobile", "clicks": 3000 }],
            "browsers": [],
            "top_codes": [{ "value": "q4", "clicks": 1200 }],
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let stats = client.project().stats(30).await.expect("stats succeeds");
    assert_eq!(stats.total_clicks, 5000);
    assert_eq!(stats.daily.len(), 1);
    assert_eq!(stats.daily[0].clicks, 12);
    assert_eq!(stats.top_codes[0].value, "q4");
}

#[tokio::test]
async fn project_stats_surfaces_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me/stats"))
        .respond_with(ResponseTemplate::new(429).set_body_json(serde_json::json!({
            "code": "rate_limited",
            "message": "slow down",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client.project().stats(30).await.expect_err("429 errors");
    assert!(err.is_rate_limited());
}

#[tokio::test]
async fn project_me_returns_project_metadata() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me"))
        .respond_with(ResponseTemplate::new(200).set_body_json(serde_json::json!({
            "id": "prj_abc",
            "name": "Acme Corp",
            "slug": "acme-corp",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let me = client.project().me().await.expect("me succeeds");
    assert_eq!(me.id, "prj_abc");
    assert_eq!(me.name, "Acme Corp");
    assert_eq!(me.slug, "acme-corp");
}

#[tokio::test]
async fn project_me_surfaces_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/projects/me"))
        .respond_with(ResponseTemplate::new(403).set_body_json(serde_json::json!({
            "code": "forbidden",
            "message": "insufficient scope",
        })))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    let err = client.project().me().await.expect_err("403 errors");
    assert_eq!(err.code(), "forbidden");
}

// ─── URL-encoding edge cases ────────────────────────────────────────────────

#[tokio::test]
async fn encodes_a_space_in_the_code() {
    assert_code_path("hello world", "/v1/links/hello%20world").await;
}

#[tokio::test]
async fn encodes_a_plus_in_the_code() {
    assert_code_path("a+b", "/v1/links/a%2Bb").await;
}

#[tokio::test]
async fn encodes_non_ascii_in_the_code() {
    assert_code_path("café", "/v1/links/caf%C3%A9").await;
}

#[tokio::test]
async fn encodes_a_slash_in_the_code() {
    assert_code_path("go/promo", "/v1/links/go%2Fpromo").await;
}

/// Mount a `get` on the encoded path and confirm the client targets it.
async fn assert_code_path(code: &str, encoded_path: &str) {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path(encoded_path.to_string()))
        .respond_with(ResponseTemplate::new(200).set_body_json(link_json("ok")))
        .mount(&server)
        .await;

    let client = client_for(&server).await;
    client
        .links()
        .get(code)
        .await
        .unwrap_or_else(|e| panic!("get({code}) should hit {encoded_path}: {e}"));
}
