//! Core HTTP client — the [`Rerout`] struct and its [`ClientBuilder`].
//!
//! All public methods are async and require a Tokio runtime.

use std::time::Duration;

use reqwest::{Client as HttpClient, Method, StatusCode};
use serde::Serialize;
use url::Url;

use crate::error::{ReroutError, Result, build_api_error};
use crate::links::Links;
use crate::project::Project;
use crate::qr::Qr;

/// Default production API base URL.
pub const DEFAULT_BASE_URL: &str = "https://api.rerout.co";

/// Default request timeout — 30 seconds.
pub const DEFAULT_TIMEOUT_SECONDS: u64 = 30;

/// HTTP method enum for the internal `request` plumbing. Mirrors what the
/// namespaces actually use — keeps the type-safe matrix small.
#[derive(Debug, Clone, Copy)]
pub(crate) enum HttpMethod {
    Get,
    Post,
    Patch,
    Delete,
}

impl HttpMethod {
    fn as_reqwest(self) -> Method {
        match self {
            HttpMethod::Get => Method::GET,
            HttpMethod::Post => Method::POST,
            HttpMethod::Patch => Method::PATCH,
            HttpMethod::Delete => Method::DELETE,
        }
    }
}

/// Builder for [`Rerout`].
///
/// ```no_run
/// use std::time::Duration;
/// use rerout::Rerout;
///
/// # async fn run() -> Result<(), rerout::ReroutError> {
/// let rerout = Rerout::builder("rrk_xxx")
///     .base_url("https://api.staging.rerout.co")?
///     .timeout(Duration::from_secs(15))
///     .build()?;
/// # let _ = rerout;
/// # Ok(())
/// # }
/// ```
#[derive(Debug)]
pub struct ClientBuilder {
    api_key: String,
    base_url: String,
    http_client: Option<HttpClient>,
    timeout: Duration,
    user_agent: Option<String>,
}

impl ClientBuilder {
    /// Start a builder from the project API key. Use [`Rerout::builder`].
    pub(crate) fn new(api_key: impl Into<String>) -> Self {
        Self {
            api_key: api_key.into(),
            base_url: DEFAULT_BASE_URL.to_string(),
            http_client: None,
            timeout: Duration::from_secs(DEFAULT_TIMEOUT_SECONDS),
            user_agent: None,
        }
    }

    /// Override the API base URL. Trailing slashes are trimmed.
    ///
    /// Returns an error if the URL fails to parse.
    pub fn base_url(mut self, base_url: impl Into<String>) -> Result<Self> {
        let raw = base_url.into();
        let trimmed = raw.trim_end_matches('/').to_string();
        if trimmed.is_empty() {
            return Err(ReroutError::Config {
                code: "invalid_base_url".to_string(),
                message: "base_url cannot be empty.".to_string(),
            });
        }
        Url::parse(&trimmed).map_err(|e| ReroutError::Config {
            code: "invalid_base_url".to_string(),
            message: format!("base_url is not a valid URL: {e}"),
        })?;
        self.base_url = trimmed;
        Ok(self)
    }

    /// Override the request timeout. Applies to each individual request.
    #[must_use]
    pub fn timeout(mut self, timeout: Duration) -> Self {
        self.timeout = timeout;
        self
    }

    /// Inject a pre-configured [`reqwest::Client`] — useful for shared
    /// connection pools, proxies, custom TLS, or test recording.
    #[must_use]
    pub fn http_client(mut self, http_client: HttpClient) -> Self {
        self.http_client = Some(http_client);
        self
    }

    /// Override the `User-Agent` header sent with every request.
    ///
    /// Has no effect when a custom [`reqwest::Client`] is provided via
    /// [`ClientBuilder::http_client`] — supply the UA on that client instead.
    #[must_use]
    pub fn user_agent(mut self, user_agent: impl Into<String>) -> Self {
        self.user_agent = Some(user_agent.into());
        self
    }

    /// Finalize the configuration and produce a [`Rerout`] client.
    pub fn build(self) -> Result<Rerout> {
        if self.api_key.trim().is_empty() {
            return Err(ReroutError::Config {
                code: "missing_api_key".to_string(),
                message: "A project API key is required to construct Rerout.".to_string(),
            });
        }
        let http_client = match self.http_client {
            Some(client) => client,
            None => {
                let mut builder = HttpClient::builder().timeout(self.timeout);
                if let Some(ua) = &self.user_agent {
                    builder = builder.user_agent(ua);
                }
                builder.build().map_err(|e| ReroutError::Config {
                    code: "http_client_build_failed".to_string(),
                    message: format!("Could not construct reqwest client: {e}"),
                })?
            }
        };
        Ok(Rerout {
            api_key: self.api_key,
            base_url: self.base_url,
            http_client,
        })
    }
}

/// The Rerout API client.
///
/// Construct via [`Rerout::builder`]. Cheap to clone — the inner
/// [`reqwest::Client`] uses an `Arc` and shares a connection pool.
///
/// ```no_run
/// use rerout::{Rerout, CreateLinkInput};
///
/// # async fn run() -> Result<(), rerout::ReroutError> {
/// let rerout = Rerout::builder("rrk_xxx").build()?;
///
/// let link = rerout
///     .links()
///     .create(&CreateLinkInput::new("https://example.com/sale"))
///     .await?;
///
/// println!("Short URL: {}", link.short_url);
/// # Ok(())
/// # }
/// ```
#[derive(Debug, Clone)]
pub struct Rerout {
    api_key: String,
    base_url: String,
    http_client: HttpClient,
}

impl Rerout {
    /// Start a [`ClientBuilder`] from the project API key (`rrk_…`).
    pub fn builder(api_key: impl Into<String>) -> ClientBuilder {
        ClientBuilder::new(api_key)
    }

    /// Convenience — build a client with default settings from the API key.
    pub fn new(api_key: impl Into<String>) -> Result<Self> {
        Self::builder(api_key).build()
    }

    /// Access the link operations namespace.
    pub fn links(&self) -> Links<'_> {
        Links::new(self)
    }

    /// Access the project-level namespace.
    pub fn project(&self) -> Project<'_> {
        Project::new(self)
    }

    /// Access the QR helpers namespace.
    pub fn qr(&self) -> Qr<'_> {
        Qr::new(self)
    }

    /// The resolved base URL — trailing slashes trimmed.
    pub fn base_url(&self) -> &str {
        &self.base_url
    }

    // ─── Internal plumbing ──────────────────────────────────────────────

    /// Send a JSON-bodied request and decode the response into `T`.
    pub(crate) async fn request_json<T, B>(
        &self,
        method: HttpMethod,
        path: &str,
        query: Option<&[(&str, String)]>,
        body: Option<&B>,
    ) -> Result<T>
    where
        T: serde::de::DeserializeOwned,
        B: Serialize + ?Sized,
    {
        let text = self.send_request(method, path, query, body).await?;
        if text.is_empty() {
            return Err(ReroutError::Unexpected {
                status: 200,
                message: "Rerout returned an empty success body where JSON was expected."
                    .to_string(),
            });
        }
        serde_json::from_str::<T>(&text).map_err(|e| ReroutError::Decode {
            message: format!("Failed to decode response: {e}"),
        })
    }

    /// Send a request and return the raw response body as a `String`.
    pub(crate) async fn request_text<B>(
        &self,
        method: HttpMethod,
        path: &str,
        query: Option<&[(&str, String)]>,
        body: Option<&B>,
    ) -> Result<String>
    where
        B: Serialize + ?Sized,
    {
        self.send_request(method, path, query, body).await
    }

    async fn send_request<B>(
        &self,
        method: HttpMethod,
        path: &str,
        query: Option<&[(&str, String)]>,
        body: Option<&B>,
    ) -> Result<String>
    where
        B: Serialize + ?Sized,
    {
        let url = format!("{}{}", self.base_url, path);
        let mut request = self
            .http_client
            .request(method.as_reqwest(), &url)
            .bearer_auth(&self.api_key)
            .header(reqwest::header::ACCEPT, "application/json");

        if let Some(pairs) = query
            && !pairs.is_empty()
        {
            request = request.query(pairs);
        }

        if let Some(payload) = body {
            request = request
                .header(reqwest::header::CONTENT_TYPE, "application/json")
                .json(payload);
        }

        let response = request.send().await.map_err(map_request_error)?;
        let status = response.status();
        let text = response.text().await.map_err(|e| ReroutError::Network {
            message: format!("Failed to read response body: {e}"),
        })?;

        if status.is_success() {
            Ok(text)
        } else {
            Err(build_api_error(status.as_u16(), &text))
        }
    }
}

fn map_request_error(error: reqwest::Error) -> ReroutError {
    if error.is_timeout() {
        return ReroutError::Timeout {
            message: error.to_string(),
        };
    }
    if let Some(status) = error.status()
        && status != StatusCode::OK
    {
        return ReroutError::Api {
            code: crate::error::synthetic_code_for_status(status.as_u16()).to_string(),
            status: status.as_u16(),
            message: error.to_string(),
            extra: Box::default(),
        };
    }
    ReroutError::Network {
        message: error.to_string(),
    }
}
