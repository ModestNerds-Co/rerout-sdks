//! `conversions` namespace — record conversion events against clicks.
//!
//! Conversions attribute downstream value (purchases, sign-ups) back to the
//! click that drove them, against `/v1/conversions`.

use crate::client::{HttpMethod, Rerout};
use crate::error::Result;
use crate::models::{ConversionResult, RecordConversionInput};

/// Conversion-tracking namespace. Reached via [`Rerout::conversions`].
#[derive(Debug, Clone)]
pub struct Conversions<'a> {
    client: &'a Rerout,
}

impl<'a> Conversions<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// Record a conversion event against a click.
    ///
    /// `POST /v1/conversions` with [`RecordConversionInput`] as the JSON body.
    /// The call is idempotent on the `(click_id, event_name)` pair: the
    /// returned [`ConversionResult`] reports whether a new row was `recorded`
    /// and whether the event was a `duplicate` of one already stored.
    pub async fn record(&self, input: &RecordConversionInput) -> Result<ConversionResult> {
        self.client
            .request_json::<ConversionResult, _>(
                HttpMethod::Post,
                "/v1/conversions",
                None,
                Some(input),
            )
            .await
    }
}
