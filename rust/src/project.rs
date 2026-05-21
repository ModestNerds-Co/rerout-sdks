//! `project` namespace — aggregate stats and project metadata.

use crate::client::{HttpMethod, Rerout};
use crate::error::Result;
use crate::models::{ProjectInfo, ProjectStats};

/// Project-level namespace. Reached via [`Rerout::project`].
#[derive(Debug, Clone)]
pub struct Project<'a> {
    client: &'a Rerout,
}

impl<'a> Project<'a> {
    pub(crate) fn new(client: &'a Rerout) -> Self {
        Self { client }
    }

    /// Aggregate stats across every link in the project. Defaults to 30 days.
    pub async fn stats(&self, days: u32) -> Result<ProjectStats> {
        let query = [("days", days.to_string())];
        self.client
            .request_json::<ProjectStats, ()>(
                HttpMethod::Get,
                "/v1/projects/me/stats",
                Some(&query),
                None,
            )
            .await
    }

    /// Info about the project that owns the current API key.
    pub async fn me(&self) -> Result<ProjectInfo> {
        self.client
            .request_json::<ProjectInfo, ()>(HttpMethod::Get, "/v1/projects/me", None, None)
            .await
    }
}
