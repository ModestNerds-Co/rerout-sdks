//! Error type for the Rerout SDK.
//!
//! Every public function returns [`Result<T, ReroutError>`]. The error
//! distinguishes between API failures (the server replied with a non-2xx),
//! transport failures (network, timeout, decode), and unexpected responses.

use serde::{Deserialize, Serialize};

/// Convenience alias for `Result<T, ReroutError>`.
pub type Result<T> = std::result::Result<T, ReroutError>;

/// Extra structured fields returned alongside an API error response.
///
/// The Rerout API may surface a `path` and `timestamp` for diagnostics, and an
/// optional `details` object with field-level validation hints.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct ApiErrorDetails {
    /// Request path the server attached to the error, when supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Server-side timestamp (ISO 8601), when supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timestamp: Option<String>,
    /// Raw `details` field from the JSON envelope, when supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub details: Option<serde_json::Value>,
}

/// Every error surfaced by the SDK.
///
/// Match on the variant for branching logic, and read [`ReroutError::code`]
/// for the stable string identifier (e.g. `bad_target_url`, `rate_limited`).
#[derive(Debug, thiserror::Error)]
pub enum ReroutError {
    /// The server replied with a non-2xx status.
    ///
    /// `code` is the stable identifier — preferring the API's `code` when one
    /// was provided in the JSON body, falling back to a synthetic
    /// status-derived code otherwise.
    #[error("Rerout API error ({code}, HTTP {status}): {message}")]
    Api {
        /// Stable error identifier (e.g. `bad_target_url`, `rate_limited`).
        code: String,
        /// HTTP status code returned by the server.
        status: u16,
        /// Human-readable error message.
        message: String,
        /// Extra fields (`path`, `timestamp`, `details`) when supplied.
        ///
        /// Boxed to keep the [`ReroutError`] enum compact — the optional
        /// diagnostics rarely matter on the hot path.
        extra: Box<ApiErrorDetails>,
    },

    /// The request never reached the server (DNS, TLS, connection reset, ...).
    #[error("Rerout network error: {message}")]
    Network {
        /// Reason from the transport layer.
        message: String,
    },

    /// The request was aborted after exceeding the client timeout.
    #[error("Rerout request timed out: {message}")]
    Timeout {
        /// Reason from the transport layer.
        message: String,
    },

    /// The server returned 2xx but the body could not be decoded as JSON.
    #[error("Rerout returned an unexpected response (HTTP {status}): {message}")]
    Unexpected {
        /// HTTP status code (always 2xx here).
        status: u16,
        /// Reason this body could not be interpreted.
        message: String,
    },

    /// A JSON body failed to decode against the expected schema.
    #[error("Rerout response decode error: {message}")]
    Decode {
        /// Reason this body could not be deserialized.
        message: String,
    },

    /// The client was misconfigured (missing API key, invalid base URL, ...).
    ///
    /// `code` mirrors the synthetic codes the API would otherwise use
    /// (e.g. `missing_api_key`, `invalid_base_url`).
    #[error("Rerout configuration error ({code}): {message}")]
    Config {
        /// Stable synthetic code for the configuration failure.
        code: String,
        /// Human-readable explanation.
        message: String,
    },
}

impl ReroutError {
    /// The stable string error code — either from the API or a synthetic one.
    ///
    /// Use this for branching logic (`if err.code() == "rate_limited" { ... }`)
    /// rather than parsing the message.
    pub fn code(&self) -> &str {
        match self {
            ReroutError::Api { code, .. } => code,
            ReroutError::Network { .. } => "network_error",
            ReroutError::Timeout { .. } => "timeout",
            ReroutError::Unexpected { .. } => "unexpected_response",
            ReroutError::Decode { .. } => "decode_error",
            ReroutError::Config { code, .. } => code,
        }
    }

    /// HTTP status code, or `0` when no response reached the client.
    pub fn status(&self) -> u16 {
        match self {
            ReroutError::Api { status, .. } | ReroutError::Unexpected { status, .. } => *status,
            ReroutError::Network { .. }
            | ReroutError::Timeout { .. }
            | ReroutError::Decode { .. }
            | ReroutError::Config { .. } => 0,
        }
    }

    /// Human-readable message — equivalent to `format!("{err}")`.
    pub fn message(&self) -> String {
        match self {
            ReroutError::Api { message, .. }
            | ReroutError::Network { message, .. }
            | ReroutError::Timeout { message, .. }
            | ReroutError::Unexpected { message, .. }
            | ReroutError::Decode { message, .. }
            | ReroutError::Config { message, .. } => message.clone(),
        }
    }

    /// `true` when this is an HTTP 429 (`rate_limited`) response.
    ///
    /// Caller should back off and retry with jitter.
    pub fn is_rate_limited(&self) -> bool {
        self.status() == 429
    }

    /// `true` when this is an HTTP 5xx response — a server-side issue.
    pub fn is_server_error(&self) -> bool {
        let s = self.status();
        (500..600).contains(&s)
    }

    /// `path` from the API error envelope, when supplied.
    pub fn path(&self) -> Option<&str> {
        match self {
            ReroutError::Api { extra, .. } => extra.path.as_deref(),
            _ => None,
        }
    }

    /// `timestamp` from the API error envelope, when supplied.
    pub fn timestamp(&self) -> Option<&str> {
        match self {
            ReroutError::Api { extra, .. } => extra.timestamp.as_deref(),
            _ => None,
        }
    }

    /// Optional `details` field from the API error envelope.
    pub fn details(&self) -> Option<&serde_json::Value> {
        match self {
            ReroutError::Api { extra, .. } => extra.details.as_ref(),
            _ => None,
        }
    }
}

/// Map an HTTP status to a synthetic error code, used when the server didn't
/// return a parseable JSON body.
pub(crate) fn synthetic_code_for_status(status: u16) -> &'static str {
    match status {
        401 => "unauthorized",
        403 => "forbidden",
        404 => "not_found",
        429 => "rate_limited",
        500..=599 => "server_error",
        _ => "client_error",
    }
}

/// Build a [`ReroutError::Api`] from an HTTP response body. Best-effort JSON
/// parse — falls back to a synthetic code when the body is empty or not JSON.
pub(crate) fn build_api_error(status: u16, body: &str) -> ReroutError {
    if body.is_empty() {
        return ReroutError::Api {
            code: synthetic_code_for_status(status).to_string(),
            status,
            message: format!("Rerout returned HTTP {status} with no body."),
            extra: Box::default(),
        };
    }
    match serde_json::from_str::<serde_json::Value>(body) {
        Ok(serde_json::Value::Object(map)) => {
            let code = map
                .get("code")
                .and_then(|v| v.as_str())
                .map(str::to_string)
                .unwrap_or_else(|| synthetic_code_for_status(status).to_string());
            let message = map
                .get("message")
                .and_then(|v| v.as_str())
                .map(str::to_string)
                .unwrap_or_else(|| format!("Rerout returned HTTP {status}."));
            let path = map.get("path").and_then(|v| v.as_str()).map(str::to_string);
            let timestamp = map
                .get("timestamp")
                .and_then(|v| v.as_str())
                .map(str::to_string);
            let details = map.get("details").cloned();
            ReroutError::Api {
                code,
                status,
                message,
                extra: Box::new(ApiErrorDetails {
                    path,
                    timestamp,
                    details,
                }),
            }
        }
        _ => ReroutError::Api {
            code: synthetic_code_for_status(status).to_string(),
            status,
            message: format!("Rerout returned HTTP {status} (non-JSON body)."),
            extra: Box::default(),
        },
    }
}
