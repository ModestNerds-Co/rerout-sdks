//! `links` namespace — create, list, get, update, delete, stats.

use crate::client::{HttpMethod, Rerout};
use crate::error::{ReroutError, Result};
use crate::models::{
    CreateLinkInput, DeleteLinkResult, Link, LinkStats, ListLinksParams, ListLinksResult,
    UpdateLinkInput,
};

/// Link operations namespace. Reached via [`Rerout::links`].
#[derive(Debug, Clone)]
pub struct Links<'a> {
    client: &'a Rerout,
}

impl<'a> Links<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// Create a new short link.
    ///
    /// `POST /v1/links` with [`CreateLinkInput`] as the JSON body.
    pub async fn create(&self, input: &CreateLinkInput) -> Result<Link> {
        self.client
            .request_json::<Link, _>(HttpMethod::Post, "/v1/links", None, Some(input))
            .await
    }

    /// Paginated list of links in the project.
    ///
    /// `GET /v1/links?cursor={cursor}&limit={limit}` — both query params
    /// optional.
    pub async fn list(&self, params: ListLinksParams) -> Result<ListLinksResult> {
        let mut query: Vec<(&'static str, String)> = Vec::new();
        if let Some(cursor) = params.cursor {
            query.push(("cursor", cursor.to_string()));
        }
        if let Some(limit) = params.limit {
            query.push(("limit", limit.to_string()));
        }
        self.client
            .request_json::<ListLinksResult, ()>(
                HttpMethod::Get,
                "/v1/links",
                if query.is_empty() { None } else { Some(&query) },
                None,
            )
            .await
    }

    /// Get a single link by code.
    pub async fn get(&self, code: &str) -> Result<Link> {
        let path = format!("/v1/links/{}", percent_encode_code(code));
        self.client
            .request_json::<Link, ()>(HttpMethod::Get, &path, None, None)
            .await
    }

    /// Patch a link. Returns a `bad_request` configuration error without
    /// hitting the network when `input.is_empty()`.
    pub async fn update(&self, code: &str, input: &UpdateLinkInput) -> Result<Link> {
        if input.is_empty() {
            return Err(ReroutError::Config {
                code: "bad_request".to_string(),
                message: "UpdateLinkInput has no fields to send.".to_string(),
            });
        }
        let path = format!("/v1/links/{}", percent_encode_code(code));
        self.client
            .request_json::<Link, _>(HttpMethod::Patch, &path, None, Some(input))
            .await
    }

    /// Soft-delete a link. The short URL stops redirecting.
    pub async fn delete(&self, code: &str) -> Result<DeleteLinkResult> {
        let path = format!("/v1/links/{}", percent_encode_code(code));
        self.client
            .request_json::<DeleteLinkResult, ()>(HttpMethod::Delete, &path, None, None)
            .await
    }

    /// Per-link click stats. Defaults to 30 days.
    pub async fn stats(&self, code: &str, days: u32) -> Result<LinkStats> {
        let path = format!("/v1/links/{}/stats", percent_encode_code(code));
        let query = [("days", days.to_string())];
        self.client
            .request_json::<LinkStats, ()>(HttpMethod::Get, &path, Some(&query), None)
            .await
    }
}

/// Percent-encode a short code for use as a path segment.
///
/// Follows RFC 3986 — escape every byte not in the path-segment unreserved
/// set. This matches `encodeURIComponent` in JavaScript closely enough that
/// `hello world` → `hello%20world`, `a+b` → `a%2Bb`, `go/promo` → `go%2Fpromo`,
/// `café` → `caf%C3%A9`.
fn percent_encode_code(code: &str) -> String {
    let mut out = String::with_capacity(code.len());
    for byte in code.bytes() {
        if is_path_unreserved(byte) {
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
fn is_path_unreserved(byte: u8) -> bool {
    matches!(byte, b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~')
}

#[inline]
fn hex_upper(nibble: u8) -> char {
    match nibble {
        0..=9 => (b'0' + nibble) as char,
        10..=15 => (b'A' + (nibble - 10)) as char,
        _ => unreachable!("nibble out of range"),
    }
}

#[cfg(test)]
mod tests {
    use super::percent_encode_code;

    #[test]
    fn encodes_spaces() {
        assert_eq!(percent_encode_code("hello world"), "hello%20world");
    }

    #[test]
    fn encodes_plus() {
        assert_eq!(percent_encode_code("a+b"), "a%2Bb");
    }

    #[test]
    fn encodes_slash() {
        assert_eq!(percent_encode_code("go/promo"), "go%2Fpromo");
    }

    #[test]
    fn encodes_non_ascii() {
        assert_eq!(percent_encode_code("café"), "caf%C3%A9");
    }

    #[test]
    fn leaves_unreserved_intact() {
        assert_eq!(percent_encode_code("q4_test.code-1~"), "q4_test.code-1~");
    }
}
