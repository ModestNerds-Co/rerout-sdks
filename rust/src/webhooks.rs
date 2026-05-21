//! Webhook signature verification — HMAC-SHA256 with constant-time compare.
//!
//! Rerout signs every delivery as `t={unix_seconds},v1={hex_hmac_sha256}`
//! where the HMAC is computed over `"{timestamp}.{raw_body}"` keyed by the
//! endpoint signing secret. This module exposes a single free function that
//! returns `true` if and only if the signature is valid.

use std::time::{SystemTime, UNIX_EPOCH};

use hmac::{Hmac, Mac};
use sha2::Sha256;
use subtle::ConstantTimeEq;

type HmacSha256 = Hmac<Sha256>;

/// Default tolerance window (in seconds) between the `t=` timestamp and the
/// current time. Five minutes — protects against captured-replay attacks.
pub const DEFAULT_TOLERANCE_SECONDS: i64 = 300;

/// Verify a Rerout webhook signature.
///
/// # Parameters
///
/// - `raw_body` — the raw, unmodified request body, exactly as received.
/// - `signature_header` — the value of the `X-Rerout-Signature` header.
/// - `secret` — the endpoint signing secret (`whsec_…`).
/// - `tolerance_seconds` — window in seconds the timestamp may drift. Pass `0`
///   to disable the staleness check.
/// - `now` — optional override for the current unix timestamp. Useful for
///   tests; defaults to `SystemTime::now()`.
///
/// Returns `true` only when the header parses cleanly, the timestamp is within
/// `tolerance_seconds` of `now`, and the computed HMAC matches the supplied
/// `v1` in constant time. Returns `false` for every malformed input —
/// callers should treat `false` as "reject this delivery".
///
/// # Example
///
/// ```
/// use rerout::webhooks::{verify_signature, DEFAULT_TOLERANCE_SECONDS};
/// # use hmac::{Hmac, Mac};
/// # use sha2::Sha256;
/// # type HmacSha256 = Hmac<Sha256>;
/// let secret = "whsec_test";
/// let body = r#"{"event":"link.created"}"#;
/// let ts = 1_700_000_000_i64;
/// # let mut mac = HmacSha256::new_from_slice(secret.as_bytes()).unwrap();
/// # mac.update(format!("{ts}.{body}").as_bytes());
/// # let sig = hex::encode(mac.finalize().into_bytes());
/// let header = format!("t={ts},v1={sig}");
/// let ok = verify_signature(
///     body,
///     &header,
///     secret,
///     DEFAULT_TOLERANCE_SECONDS,
///     Some(ts), // pretend "now" is the signing moment
/// );
/// assert!(ok);
/// ```
pub fn verify_signature(
    raw_body: &str,
    signature_header: &str,
    secret: &str,
    tolerance_seconds: i64,
    now: Option<i64>,
) -> bool {
    if signature_header.is_empty() || secret.is_empty() {
        return false;
    }

    let parsed = match parse_signature_header(signature_header) {
        Some(parts) => parts,
        None => return false,
    };

    if tolerance_seconds > 0 {
        let now_seconds = now.unwrap_or_else(current_unix_seconds);
        let drift = now_seconds.saturating_sub(parsed.timestamp).abs();
        if drift > tolerance_seconds {
            return false;
        }
    }

    let mut mac = match HmacSha256::new_from_slice(secret.as_bytes()) {
        Ok(m) => m,
        Err(_) => return false,
    };
    let signed_payload = format!("{}.{}", parsed.timestamp, raw_body);
    mac.update(signed_payload.as_bytes());
    let expected = mac.finalize().into_bytes();

    let actual = match hex_decode(&parsed.v1) {
        Some(bytes) => bytes,
        None => return false,
    };
    if expected.len() != actual.len() {
        return false;
    }
    expected.ct_eq(&actual).into()
}

struct ParsedSignature {
    timestamp: i64,
    v1: String,
}

fn parse_signature_header(header: &str) -> Option<ParsedSignature> {
    let mut timestamp: Option<i64> = None;
    let mut v1: Option<String> = None;
    for segment in header.split(',') {
        let Some(eq) = segment.find('=') else {
            continue;
        };
        if eq == 0 {
            continue;
        }
        let key = segment[..eq].trim().to_ascii_lowercase();
        let value = segment[eq + 1..].trim();
        if value.is_empty() {
            continue;
        }
        match key.as_str() {
            "t" => {
                if let Ok(parsed) = value.parse::<i64>()
                    && parsed > 0
                {
                    timestamp = Some(parsed);
                }
            }
            "v1" => {
                v1 = Some(value.to_string());
            }
            _ => {}
        }
    }
    Some(ParsedSignature {
        timestamp: timestamp?,
        v1: v1?,
    })
}

fn hex_decode(s: &str) -> Option<Vec<u8>> {
    if s.is_empty() || s.len() % 2 != 0 {
        return None;
    }
    if !s.bytes().all(|b| b.is_ascii_hexdigit()) {
        return None;
    }
    hex::decode(s).ok()
}

fn current_unix_seconds() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs() as i64)
        .unwrap_or(0)
}
