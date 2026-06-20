//! `tags` namespace — list, create, update, delete project tags.
//!
//! This is the API-key-authenticated tag management surface under
//! `/v1/projects/me/tags`. The project is resolved from the API key — there is
//! no project id in the path. Tags labelled on a [`crate::Link`] are read via
//! that link's `tags`; this namespace manages the tags themselves.

use crate::client::{HttpMethod, Rerout};
use crate::error::Result;
use crate::models::{CreateTagInput, DeleteTagResult, ListTagsResult, Tag, UpdateTagInput};

/// Tag management namespace. Reached via [`Rerout::tags`].
#[derive(Debug, Clone)]
pub struct Tags<'a> {
    client: &'a Rerout,
}

impl<'a> Tags<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// List the project's tags with their live link counts.
    ///
    /// `GET /v1/projects/me/tags`. Each entry in the returned
    /// [`ListTagsResult::tags`] is a [`crate::TagSummary`] — the tag plus a
    /// `link_count` of the live (non-deleted) links it is attached to.
    pub async fn list(&self) -> Result<ListTagsResult> {
        self.client
            .request_json::<ListTagsResult, ()>(HttpMethod::Get, "/v1/projects/me/tags", None, None)
            .await
    }

    /// Create a tag.
    ///
    /// `POST /v1/projects/me/tags` with [`CreateTagInput`] as the JSON body.
    /// `color` is optional — the server validates it against its palette and
    /// defaults to `teal`. The returned [`Tag`] has no `link_count`.
    pub async fn create(&self, input: &CreateTagInput) -> Result<Tag> {
        self.client
            .request_json::<Tag, _>(HttpMethod::Post, "/v1/projects/me/tags", None, Some(input))
            .await
    }

    /// Update a tag's name and/or color.
    ///
    /// `PATCH /v1/projects/me/tags/:tag_id`. Only the fields set on
    /// [`UpdateTagInput`] are sent; omitted fields are left unchanged. There is
    /// no client-side empty-payload check — the server returns `400` for a
    /// fully empty patch.
    pub async fn update(&self, tag_id: &str, input: &UpdateTagInput) -> Result<Tag> {
        let path = format!("/v1/projects/me/tags/{}", percent_encode_segment(tag_id));
        self.client
            .request_json::<Tag, _>(HttpMethod::Patch, &path, None, Some(input))
            .await
    }

    /// Delete a tag and drop its assignments from all links.
    ///
    /// `DELETE /v1/projects/me/tags/:tag_id`.
    pub async fn delete(&self, tag_id: &str) -> Result<DeleteTagResult> {
        let path = format!("/v1/projects/me/tags/{}", percent_encode_segment(tag_id));
        self.client
            .request_json::<DeleteTagResult, ()>(HttpMethod::Delete, &path, None, None)
            .await
    }
}

/// Percent-encode a tag id for use as a path segment.
///
/// Mirrors the `links` / `webhooks` namespaces — escape every byte not in the
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
        assert_eq!(percent_encode_segment("tag_abc123"), "tag_abc123");
    }

    #[test]
    fn encodes_slash() {
        assert_eq!(percent_encode_segment("tag_a/b"), "tag_a%2Fb");
    }

    #[test]
    fn encodes_spaces() {
        assert_eq!(percent_encode_segment("tag a"), "tag%20a");
    }
}
