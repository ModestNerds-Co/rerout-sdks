//! Integration tests for [`verify_rerout_signature`] — valid signatures,
//! tampering, replay tolerance, malformed headers, and casing variations.

use hmac::{Hmac, Mac};
use rerout::webhooks::{DEFAULT_TOLERANCE_SECONDS, verify_rerout_signature};
use sha2::Sha256;

type HmacSha256 = Hmac<Sha256>;

/// Compute the hex HMAC-SHA256 over `"{ts}.{body}"` keyed by `secret`.
fn sign(secret: &str, ts: i64, body: &str) -> String {
    let mut mac = HmacSha256::new_from_slice(secret.as_bytes()).expect("hmac key");
    mac.update(format!("{ts}.{body}").as_bytes());
    hex::encode(mac.finalize().into_bytes())
}

/// Build a `t={ts},v1={hmac}` header for `body` signed with `secret`.
fn header(secret: &str, ts: i64, body: &str) -> String {
    format!("t={ts},v1={}", sign(secret, ts, body))
}

const SECRET: &str = "whsec_test_secret";
const BODY: &str = r#"{"event":"link.created","data":{"code":"q4"}}"#;
const NOW: i64 = 1_700_000_000;

// ─── Valid signatures ───────────────────────────────────────────────────────

#[test]
fn accepts_a_freshly_signed_payload() {
    let header = header(SECRET, NOW, BODY);
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn accepts_a_signature_using_the_real_clock_when_fresh() {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64;
    let header = header(SECRET, now, BODY);
    // `None` makes the verifier read the system clock.
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        None,
    ));
}

#[test]
fn accepts_an_empty_body_payload() {
    let header = header(SECRET, NOW, "");
    assert!(verify_rerout_signature(
        "",
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

// ─── Rejection — wrong secret / tampering ───────────────────────────────────

#[test]
fn rejects_a_signature_made_with_a_different_secret() {
    let header = header("whsec_other_secret", NOW, BODY);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_tampered_body_with_an_extra_space() {
    let header = header(SECRET, NOW, BODY);
    let tampered = format!("{BODY} ");
    assert!(!verify_rerout_signature(
        &tampered,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_tampered_body_with_changed_content() {
    let header = header(SECRET, NOW, BODY);
    let tampered = r#"{"event":"link.deleted","data":{"code":"q4"}}"#;
    assert!(!verify_rerout_signature(
        tampered,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_when_the_timestamp_was_swapped_after_signing() {
    // Header timestamp no longer matches the one folded into the HMAC.
    let signed_at = NOW;
    let hmac = sign(SECRET, signed_at, BODY);
    let header = format!("t={},v1={hmac}", signed_at + 1);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

// ─── Replay tolerance ───────────────────────────────────────────────────────

#[test]
fn rejects_a_payload_older_than_the_tolerance() {
    let stale = NOW - DEFAULT_TOLERANCE_SECONDS - 1;
    let header = header(SECRET, stale, BODY);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_payload_from_too_far_in_the_future() {
    let future = NOW + DEFAULT_TOLERANCE_SECONDS + 1;
    let header = header(SECRET, future, BODY);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn accepts_a_payload_exactly_at_the_tolerance_boundary() {
    let edge = NOW - DEFAULT_TOLERANCE_SECONDS;
    let header = header(SECRET, edge, BODY);
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn accepts_a_future_payload_exactly_at_the_tolerance_boundary() {
    let edge = NOW + DEFAULT_TOLERANCE_SECONDS;
    let header = header(SECRET, edge, BODY);
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn tolerance_zero_disables_the_timestamp_check() {
    // A timestamp years in the past still verifies when tolerance is 0.
    let ancient = NOW - 10_000_000;
    let header = header(SECRET, ancient, BODY);
    assert!(verify_rerout_signature(BODY, &header, SECRET, 0, Some(NOW)));
}

#[test]
fn tolerance_zero_still_rejects_a_bad_hmac() {
    // Disabling the clock check must not weaken HMAC verification.
    let header = header("whsec_wrong", NOW, BODY);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        0,
        Some(NOW)
    ));
}

#[test]
fn a_custom_tolerance_window_is_honoured() {
    let stale = NOW - 100;
    let header = header(SECRET, stale, BODY);
    // 60s window — the 100s-old payload must be rejected.
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        60,
        Some(NOW)
    ));
    // 120s window — the same payload is now in range.
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        120,
        Some(NOW)
    ));
}

// ─── Malformed headers ──────────────────────────────────────────────────────

#[test]
fn rejects_an_empty_header() {
    assert!(!verify_rerout_signature(
        BODY,
        "",
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_garbage_header() {
    assert!(!verify_rerout_signature(
        BODY,
        "garbage",
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_header_with_empty_values() {
    assert!(!verify_rerout_signature(
        BODY,
        "t=,v1=abc",
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_header_missing_the_timestamp() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!("v1={hmac}");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_header_missing_the_signature() {
    let header = format!("t={NOW}");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_non_numeric_timestamp() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!("t=notanumber,v1={hmac}");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_non_positive_timestamp() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!("t=0,v1={hmac}");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_non_hex_signature() {
    let header = format!("t={NOW},v1=zzzznothex");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_an_odd_length_signature() {
    // 65 hex chars — valid characters but an odd count cannot be byte-decoded.
    let header = format!("t={NOW},v1={}", "a".repeat(65));
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_a_correctly_shaped_but_wrong_length_signature() {
    // Even-length valid hex, but not 32 bytes — must fail the length check.
    let header = format!("t={NOW},v1=abcdef");
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

// ─── Casing variations ──────────────────────────────────────────────────────

#[test]
fn accepts_uppercase_header_keys() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!("T={NOW},V1={hmac}");
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn accepts_mixed_case_header_keys_and_uppercase_hex() {
    let hmac = sign(SECRET, NOW, BODY).to_uppercase();
    let header = format!("T={NOW},v1={hmac}");
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn tolerates_whitespace_around_segments() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!(" t = {NOW} , v1 = {hmac} ");
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn ignores_unknown_extra_segments() {
    let hmac = sign(SECRET, NOW, BODY);
    let header = format!("t={NOW},v1={hmac},v0=legacy");
    assert!(verify_rerout_signature(
        BODY,
        &header,
        SECRET,
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

// ─── Empty secret ───────────────────────────────────────────────────────────

#[test]
fn rejects_an_empty_secret() {
    let header = header(SECRET, NOW, BODY);
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        "",
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

#[test]
fn rejects_an_empty_secret_even_with_a_valid_shaped_header() {
    // Reaching the empty-secret guard early matters: never compute an HMAC
    // with an empty key.
    let header = format!("t={NOW},v1={}", "a".repeat(64));
    assert!(!verify_rerout_signature(
        BODY,
        &header,
        "",
        DEFAULT_TOLERANCE_SECONDS,
        Some(NOW),
    ));
}

// ─── Default tolerance constant ─────────────────────────────────────────────

#[test]
fn default_tolerance_is_five_minutes() {
    assert_eq!(DEFAULT_TOLERANCE_SECONDS, 300);
}
