//! Integration tests for the `qr` namespace — the pure URL builder and the
//! authenticated SVG fetch.

use rerout::{QrEcc, QrOptions, QrRefresh, Rerout, build_qr_url};
use url::Url;
use wiremock::matchers::{header, method, path, query_param};
use wiremock::{Mock, MockServer, ResponseTemplate};

fn client(base_url: &str) -> Rerout {
    Rerout::builder("rrk_test")
        .base_url(base_url)
        .unwrap()
        .build()
        .unwrap()
}

/// Parse a built URL and collect its query pairs into a sorted vec for
/// order-independent assertions.
fn query_pairs(url: &str) -> Vec<(String, String)> {
    let parsed = Url::parse(url).expect("built URL must parse");
    let mut pairs: Vec<(String, String)> = parsed
        .query_pairs()
        .map(|(k, v)| (k.into_owned(), v.into_owned()))
        .collect();
    pairs.sort();
    pairs
}

// ─── url() — bare ───────────────────────────────────────────────────────────

#[test]
fn url_with_no_options_is_a_bare_endpoint() {
    let client = client("https://api.rerout.co");
    let url = client.qr().url("q4", &QrOptions::new()).unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/q4/qr");
}

#[test]
fn url_honours_a_custom_base_url() {
    let client = client("https://api.staging.rerout.co");
    let url = client.qr().url("q4", &QrOptions::new()).unwrap();
    assert_eq!(url, "https://api.staging.rerout.co/v1/links/q4/qr");
}

#[test]
fn url_trims_a_trailing_slash_from_the_base() {
    // base_url() already trims; build_qr_url trims defensively too.
    let url = build_qr_url("https://api.rerout.co/", "q4", &QrOptions::new()).unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/q4/qr");
}

// ─── url() — every option ───────────────────────────────────────────────────

#[test]
fn url_emits_every_option() {
    let client = client("https://api.rerout.co");
    let options = QrOptions::new()
        .with_size(12)
        .with_margin(2)
        .with_ecc(QrEcc::H)
        .with_domain("go.brand.com")
        .with_refresh(QrRefresh::On);
    let url = client.qr().url("q4", &options).unwrap();

    assert!(url.starts_with("https://api.rerout.co/v1/links/q4/qr?"));
    assert_eq!(
        query_pairs(&url),
        vec![
            ("domain".to_string(), "go.brand.com".to_string()),
            ("ecc".to_string(), "H".to_string()),
            ("margin".to_string(), "2".to_string()),
            ("refresh".to_string(), "1".to_string()),
            ("size".to_string(), "12".to_string()),
        ],
    );
}

#[test]
fn url_emits_only_the_options_that_are_set() {
    let client = client("https://api.rerout.co");
    let options = QrOptions::new().with_size(20);
    let url = client.qr().url("q4", &options).unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/q4/qr?size=20");
}

#[test]
fn url_emits_each_ecc_level() {
    let client = client("https://api.rerout.co");
    for (level, wire) in [
        (QrEcc::L, "L"),
        (QrEcc::M, "M"),
        (QrEcc::Q, "Q"),
        (QrEcc::H, "H"),
    ] {
        let url = client
            .qr()
            .url("q4", &QrOptions::new().with_ecc(level))
            .unwrap();
        assert_eq!(
            url,
            format!("https://api.rerout.co/v1/links/q4/qr?ecc={wire}")
        );
    }
}

// ─── url() — refresh handling ───────────────────────────────────────────────

#[test]
fn refresh_on_is_emitted_as_one() {
    let client = client("https://api.rerout.co");
    let url = client
        .qr()
        .url("q4", &QrOptions::new().with_refresh(QrRefresh::On))
        .unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/q4/qr?refresh=1");
}

#[test]
fn refresh_value_is_emitted_verbatim() {
    let client = client("https://api.rerout.co");
    let refresh = QrRefresh::from_value("v2").unwrap();
    let url = client
        .qr()
        .url("q4", &QrOptions::new().with_refresh(refresh))
        .unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/q4/qr?refresh=v2");
}

#[test]
fn refresh_from_bool_true_maps_to_on() {
    assert_eq!(QrRefresh::from_bool(true), Some(QrRefresh::On));
}

#[test]
fn refresh_from_bool_false_maps_to_none() {
    assert_eq!(QrRefresh::from_bool(false), None);
}

#[test]
fn refresh_from_value_rejects_the_empty_string() {
    assert_eq!(QrRefresh::from_value(""), None);
}

// ─── url() — code encoding ──────────────────────────────────────────────────

#[test]
fn url_percent_encodes_the_code() {
    let client = client("https://api.rerout.co");
    let url = client.qr().url("go/promo", &QrOptions::new()).unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/go%2Fpromo/qr");
}

#[test]
fn url_percent_encodes_a_spaced_code() {
    let client = client("https://api.rerout.co");
    let url = client.qr().url("hello world", &QrOptions::new()).unwrap();
    assert_eq!(url, "https://api.rerout.co/v1/links/hello%20world/qr");
}

// ─── svg() ──────────────────────────────────────────────────────────────────

#[tokio::test]
async fn svg_fetches_the_rendered_body() {
    let server = MockServer::start().await;
    let svg = r#"<svg xmlns="http://www.w3.org/2000/svg"></svg>"#;
    Mock::given(method("GET"))
        .and(path("/v1/links/q4/qr"))
        .and(header("authorization", "Bearer rrk_test"))
        .respond_with(
            ResponseTemplate::new(200)
                .insert_header("content-type", "image/svg+xml")
                .set_body_string(svg),
        )
        .mount(&server)
        .await;

    let client = client(&server.uri());
    let body = client
        .qr()
        .svg("q4", &QrOptions::new())
        .await
        .expect("svg succeeds");
    assert_eq!(body, svg);
}

#[tokio::test]
async fn svg_forwards_options_as_query_params() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/q4/qr"))
        .and(query_param("size", "16"))
        .and(query_param("ecc", "Q"))
        .and(query_param("refresh", "1"))
        .respond_with(ResponseTemplate::new(200).set_body_string("<svg/>"))
        .mount(&server)
        .await;

    let client = client(&server.uri());
    let options = QrOptions::new()
        .with_size(16)
        .with_ecc(QrEcc::Q)
        .with_refresh(QrRefresh::On);
    client
        .qr()
        .svg("q4", &options)
        .await
        .expect("svg with options succeeds");
}

#[tokio::test]
async fn svg_surfaces_server_errors() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/ghost/qr"))
        .respond_with(ResponseTemplate::new(404).set_body_json(serde_json::json!({
            "code": "not_found",
            "message": "no such link",
        })))
        .mount(&server)
        .await;

    let client = client(&server.uri());
    let err = client
        .qr()
        .svg("ghost", &QrOptions::new())
        .await
        .expect_err("404 errors");
    assert_eq!(err.code(), "not_found");
    assert_eq!(err.status(), 404);
}

#[tokio::test]
async fn svg_encodes_the_code_in_the_path() {
    let server = MockServer::start().await;
    Mock::given(method("GET"))
        .and(path("/v1/links/go%2Fpromo/qr"))
        .respond_with(ResponseTemplate::new(200).set_body_string("<svg/>"))
        .mount(&server)
        .await;

    let client = client(&server.uri());
    client
        .qr()
        .svg("go/promo", &QrOptions::new())
        .await
        .expect("svg with encoded code succeeds");
}

// ─── QrOptions helpers ──────────────────────────────────────────────────────

#[test]
fn empty_options_report_is_empty() {
    assert!(QrOptions::new().is_empty());
}

#[test]
fn populated_options_are_not_empty() {
    assert!(!QrOptions::new().with_size(8).is_empty());
}
