// Package rerout — error type.
//
// Every failed Rerout API call surfaces as a *ReroutError. The Code field is
// the stable API error code (e.g. "bad_target_url", "rate_limited",
// "not_found") so callers can branch on it without parsing Message. For
// network, timeout, or non-JSON failures the SDK fills Code with a synthetic
// value (see synthCodeForStatus and the constants below).

package rerout

import (
	"errors"
	"fmt"
)

// Synthetic Code values, used when the server didn't return a JSON error body
// or the request never reached the server.
const (
	CodeUnauthorized       = "unauthorized"
	CodeForbidden          = "forbidden"
	CodeNotFound           = "not_found"
	CodeRateLimited        = "rate_limited"
	CodeServerError        = "server_error"
	CodeClientError        = "client_error"
	CodeNetworkError       = "network_error"
	CodeTimeout            = "timeout"
	CodeUnexpectedResponse = "unexpected_response"
	CodeMissingAPIKey      = "missing_api_key"
	CodeBadRequest         = "bad_request"
)

// ReroutError is the error type returned by every Client method. It implements
// the standard error interface, so it works with errors.Is / errors.As.
//
// Callers should branch on Code, not on Message:
//
//	var rerr *rerout.ReroutError
//	if errors.As(err, &rerr) {
//	    switch rerr.Code {
//	    case "bad_target_url":
//	        // ...
//	    }
//	    if rerr.IsRateLimited() {
//	        // back off & retry
//	    }
//	}
type ReroutError struct {
	// Code is the stable error code — either from the API response body or a
	// synthetic value defined above.
	Code string
	// Status is the HTTP status code, or 0 when the request never reached the
	// server (network failure, timeout, client-side validation).
	Status int
	// Message is a human-readable description of what went wrong.
	Message string
	// Path is the API path that produced the error, when known.
	Path string
	// Timestamp is the server-supplied ISO 8601 timestamp, when present.
	Timestamp string
	// Details is the parsed JSON error body or any other diagnostic payload.
	// Always safe to ignore — Code and Message are the canonical fields.
	Details any
	// Cause is the wrapped error, when this ReroutError stems from a lower-
	// level failure (network error, parse failure). Exposed via Unwrap so
	// errors.Is / errors.As reach the underlying error.
	Cause error
}

// Error implements the error interface.
func (e *ReroutError) Error() string {
	if e == nil {
		return "<nil ReroutError>"
	}
	if e.Path != "" {
		return fmt.Sprintf("rerout: %s (code=%s, status=%d, path=%s)", e.Message, e.Code, e.Status, e.Path)
	}
	return fmt.Sprintf("rerout: %s (code=%s, status=%d)", e.Message, e.Code, e.Status)
}

// Unwrap returns the wrapped cause, if any, so errors.Is / errors.As reach it.
func (e *ReroutError) Unwrap() error {
	if e == nil {
		return nil
	}
	return e.Cause
}

// IsRateLimited reports whether the failure is HTTP 429 — caller should
// back off and retry.
func (e *ReroutError) IsRateLimited() bool {
	if e == nil {
		return false
	}
	return e.Status == 429
}

// IsServerError reports whether the failure is HTTP 5xx — a server-side
// issue. Generally worth retrying after backoff.
func (e *ReroutError) IsServerError() bool {
	if e == nil {
		return false
	}
	return e.Status >= 500 && e.Status < 600
}

// AsReroutError extracts the underlying *ReroutError from err, or nil if err
// is not a ReroutError. This is a small convenience over errors.As — feel
// free to use errors.As directly.
func AsReroutError(err error) *ReroutError {
	if err == nil {
		return nil
	}
	var rerr *ReroutError
	if errors.As(err, &rerr) {
		return rerr
	}
	return nil
}

// synthCodeForStatus picks the synthetic Code value for an HTTP status that
// the server returned with no parseable JSON body.
func synthCodeForStatus(status int) string {
	switch status {
	case 401:
		return CodeUnauthorized
	case 403:
		return CodeForbidden
	case 404:
		return CodeNotFound
	case 429:
		return CodeRateLimited
	}
	if status >= 500 && status < 600 {
		return CodeServerError
	}
	return CodeClientError
}
