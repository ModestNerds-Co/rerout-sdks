//! `webhooks` namespace — create, list, delete webhook endpoints.
//!
//! This is the API-key-authenticated management surface under
//! `/v1/projects/me/webhooks`. It is distinct from the inbound signature
//! verification in [`crate::webhooks`] — that module verifies deliveries; this
//! one manages the endpoints that receive them.

use crate::client::{HttpMethod, Rerout};
use crate::error::Result;
use crate::models::{CreateWebhookInput, CreatedWebhook, DeleteWebhookResult, ListWebhooksResult};

/// Webhook endpoint management namespace. Reached via [`Rerout::webhooks`].
#[derive(Debug, Clone)]
pub struct Webhooks<'a> {
    client: &'a Rerout,
}

impl<'a> Webhooks<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// Create a webhook endpoint for the project that owns the API key.
    ///
    /// `POST /v1/projects/me/webhooks` with [`CreateWebhookInput`] as the JSON
    /// body. The returned [`CreatedWebhook::signing_secret`] is shown once —
    /// persist it to verify future deliveries.
    pub async fn create(&self, input: &CreateWebhookInput) -> Result<CreatedWebhook> {
        self.client
            .request_json::<CreatedWebhook, _>(
                HttpMethod::Post,
                "/v1/projects/me/webhooks",
                None,
                Some(input),
            )
            .await
    }

    /// List webhook endpoints and the event types the server can deliver.
    ///
    /// `GET /v1/projects/me/webhooks`.
    pub async fn list(&self) -> Result<ListWebhooksResult> {
        self.client
            .request_json::<ListWebhooksResult, ()>(
                HttpMethod::Get,
                "/v1/projects/me/webhooks",
                None,
                None,
            )
            .await
    }

    /// Soft-delete an endpoint and abandon its pending deliveries. Idempotent.
    ///
    /// `DELETE /v1/projects/me/webhooks/:endpoint_id`.
    pub async fn delete(&self, endpoint_id: &str) -> Result<DeleteWebhookResult> {
        let path = format!("/v1/projects/me/webhooks/{}", percent_encode_segment(endpoint_id));
        self.client
            .request_json::<DeleteWebhookResult, ()>(HttpMethod::Delete, &path, None, None)
            .await
    }
}

/// Percent-encode an endpoint id for use as a path segment.
///
/// Mirrors the `links` namespace encoding — escape every byte not in the
/// RFC 3986 path-segment unreserved set, matching `encodeURIComponent`.
fn percent_encode_segment(segment: &str) -> String {
    let mut out = String::with_capacity(segment.len());
    for byte in segment.bytes() {
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
    use super::percent_encode_segment;

    #[test]
    fn encodes_plain_id_unchanged() {
        assert_eq!(percent_encode_segment("wh_abc123"), "wh_abc123");
    }

    #[test]
    fn encodes_slash() {
        assert_eq!(percent_encode_segment("wh_a/b"), "wh_a%2Fb");
    }

    #[test]
    fn encodes_spaces() {
        assert_eq!(percent_encode_segment("wh a"), "wh%20a");
    }
}
