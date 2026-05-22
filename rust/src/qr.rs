//! `qr` namespace — pure URL builder and authenticated SVG fetch.

use url::Url;

use crate::client::{HttpMethod, Rerout};
use crate::error::{ReroutError, Result};
use crate::models::QrOptions;

/// QR helpers. Reached via [`Rerout::qr`].
#[derive(Debug, Clone)]
pub struct Qr<'a> {
    client: &'a Rerout,
}

impl<'a> Qr<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// Build the URL the API serves the QR SVG from.
    ///
    /// Pure — does not hit the network and does not attach the bearer token.
    /// Pass the result to an `<img>` tag, download it server-side, or send it
    /// downstream.
    pub fn url(&self, code: &str, options: &QrOptions) -> Result<String> {
        build_qr_url(self.client.base_url(), code, options)
    }

    /// Fetch the QR SVG body. Hits the same endpoint as [`Qr::url`] but
    /// attaches the bearer token. Returns the SVG text.
    pub async fn svg(&self, code: &str, options: &QrOptions) -> Result<String> {
        let path = format!("/v1/links/{}/qr", percent_encode_code(code));
        let pairs = options.to_query_pairs();
        let query: Option<&[(&str, String)]> = if pairs.is_empty() { None } else { Some(&pairs) };
        self.client
            .request_text::<()>(HttpMethod::Get, &path, query, None)
            .await
    }
}

/// Build a QR URL from an arbitrary base URL. Used internally by [`Qr::url`]
/// and exposed as a free function for callers that already have a base URL
/// in hand without constructing a full client.
pub fn build_qr_url(base_url: &str, code: &str, options: &QrOptions) -> Result<String> {
    let trimmed = base_url.trim_end_matches('/');
    let raw = format!("{}/v1/links/{}/qr", trimmed, percent_encode_code(code));
    let mut url = Url::parse(&raw).map_err(|e| ReroutError::Config {
        code: "invalid_base_url".to_string(),
        message: format!("Could not build QR URL: {e}"),
    })?;
    let pairs = options.to_query_pairs();
    if !pairs.is_empty() {
        let mut q = url.query_pairs_mut();
        for (k, v) in &pairs {
            q.append_pair(k, v);
        }
        drop(q);
    }
    Ok(url.to_string())
}

/// Path-segment percent-encoder. Kept in sync with `links::percent_encode_code`
/// — duplicated here so neither module has to make the other public.
fn percent_encode_code(code: &str) -> String {
    let mut out = String::with_capacity(code.len());
    for byte in code.bytes() {
        if matches!(byte, b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~') {
            out.push(byte as char);
        } else {
            out.push('%');
            out.push(hex_upper(byte >> 4));
            out.push(hex_upper(byte & 0x0f));
        }
    }
    out
}

#[inline]
fn hex_upper(nibble: u8) -> char {
    match nibble {
        0..=9 => (b'0' + nibble) as char,
        10..=15 => (b'A' + (nibble - 10)) as char,
        _ => unreachable!("nibble out of range"),
    }
}
