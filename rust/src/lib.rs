//! Official Rust SDK for the [Rerout](https://rerout.co) branded-link API.
//!
//! Rerout is branded link infrastructure on Cloudflare. This crate wraps the
//! HTTP API: create short links, render QR codes, read analytics, and verify
//! webhook signatures.
//!
//! Every public method is async and requires a [Tokio](https://tokio.rs)
//! runtime. The client is built through a builder and is cheap to clone — the
//! inner [`reqwest::Client`] shares a connection pool.
//!
//! # Quick start
//!
//! ```no_run
//! use rerout::{CreateLinkInput, Rerout};
//!
//! # async fn run() -> Result<(), rerout::ReroutError> {
//! let rerout = Rerout::new("rrk_live_xxx")?;
//!
//! let link = rerout
//!     .links()
//!     .create(&CreateLinkInput::new("https://example.com/q4-sale"))
//!     .await?;
//!
//! println!("Short URL: {}", link.short_url);
//! # Ok(())
//! # }
//! ```
//!
//! # Namespaces
//!
//! The [`Rerout`] client exposes six namespaces:
//!
//! - [`Rerout::links`] — create, batch-create, list, get, update, delete, and
//!   per-link stats.
//! - [`Rerout::project`] — aggregate project stats and project metadata.
//! - [`Rerout::qr`] — a pure QR URL builder plus an authenticated SVG fetch.
//! - [`Rerout::webhooks`] — create, list, and delete webhook endpoints.
//! - [`Rerout::conversions`] — record conversion events against clicks.
//! - [`Rerout::tags`] — list, create, update, and delete project tags.
//!
//! # Error handling
//!
//! Every fallible call returns [`Result<T, ReroutError>`](Result). Match on the
//! [`ReroutError`] variant for branching, or read [`ReroutError::code`] for the
//! stable string identifier the API attaches to a failure.
//!
//! # Webhook signatures
//!
//! Incoming webhook deliveries are signed. Verify them with
//! [`webhooks::verify_rerout_signature`], which performs a constant-time HMAC
//! comparison and rejects stale timestamps.

#![forbid(unsafe_code)]
#![deny(missing_docs)]
#![deny(rustdoc::broken_intra_doc_links)]

mod client;
mod conversions;
mod error;
mod links;
mod models;
mod project;
mod qr;
mod tags;
mod webhooks_management;

pub mod webhooks;

pub use client::{ClientBuilder, DEFAULT_BASE_URL, DEFAULT_TIMEOUT_SECONDS, Rerout};
pub use conversions::Conversions;
pub use error::{ApiErrorDetails, ReroutError, Result};
pub use links::Links;
pub use models::{
    AbVariant, BatchCreateLinksResult, BatchLinkInput, BatchLinkResult, ConversionResult,
    CreateAbVariantInput, CreateLinkInput, CreateTagInput, CreateWebhookInput, CreatedWebhook,
    DailyClicksPoint, DeleteLinkResult, DeleteTagResult, DeleteWebhookResult, Link, LinkStats,
    ListLinksParams, ListLinksResult, ListTagsResult, ListWebhooksResult, ProjectInfo,
    ProjectStats, QrEcc, QrOptions, QrRefresh, RecordConversionInput, RoutingRule, StatsBreakdown,
    Tag, TagSummary, UpdateLinkInput, UpdateTagInput, Webhook, WebhookPayloadFormat,
};
pub use project::Project;
pub use qr::{Qr, build_qr_url};
pub use tags::Tags;
pub use webhooks::{DEFAULT_TOLERANCE_SECONDS, verify_rerout_signature};
pub use webhooks_management::Webhooks;
