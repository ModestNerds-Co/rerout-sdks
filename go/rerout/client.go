// Package rerout — Client construction & HTTP transport.
//
// The Client is the entry point to the SDK. Construct it with NewClient and
// reach the three namespaces via the Links(), Project(), and QR() methods.
//
//	c, err := rerout.NewClient(os.Getenv("REROUT_API_KEY"))
//	if err != nil { log.Fatal(err) }
//
//	link, err := c.Links().Create(ctx, rerout.CreateLinkInput{
//	    TargetURL: "https://example.com/q4-sale",
//	})

package rerout

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

// DefaultBaseURL is the production Rerout API endpoint. Override via
// WithBaseURL for staging or self-hosted deployments.
const DefaultBaseURL = "https://api.rerout.co"

// DefaultTimeout is the per-request HTTP timeout applied when no
// http.Client is injected and WithTimeout is not used.
const DefaultTimeout = 30 * time.Second

// Client is the official Go client for the Rerout API. Construct via
// NewClient; the zero value is not usable.
//
// Clients are safe for concurrent use by multiple goroutines as long as the
// underlying http.Client is (the stdlib default is).
type Client struct {
	apiKey         string
	baseURL        string
	httpClient     *http.Client
	defaultHeaders map[string]string

	links       *Links
	project     *ProjectNS
	qr          *QR
	webhooks    *Webhooks
	conversions *Conversions
}

// Option configures a Client at construction time. See WithBaseURL,
// WithHTTPClient, WithTimeout, and WithDefaultHeaders.
type Option func(*clientConfig)

type clientConfig struct {
	baseURL        string
	httpClient     *http.Client
	timeout        time.Duration
	timeoutSet     bool
	defaultHeaders map[string]string
}

// WithBaseURL overrides the API base URL. Trailing slashes are stripped.
// Useful for staging / self-hosted setups.
func WithBaseURL(baseURL string) Option {
	return func(cfg *clientConfig) { cfg.baseURL = baseURL }
}

// WithHTTPClient injects a pre-configured *http.Client. Use this when you
// need a custom transport (proxy, instrumentation, retries) or when sharing
// a client with the rest of your application.
//
// If WithHTTPClient is supplied together with WithTimeout, the timeout option
// is ignored — set the timeout on the injected client instead.
func WithHTTPClient(c *http.Client) Option {
	return func(cfg *clientConfig) { cfg.httpClient = c }
}

// WithTimeout sets a per-request timeout. Defaults to DefaultTimeout
// (30s). Ignored when WithHTTPClient is also supplied.
func WithTimeout(d time.Duration) Option {
	return func(cfg *clientConfig) {
		cfg.timeout = d
		cfg.timeoutSet = true
	}
}

// WithDefaultHeaders sets headers attached to every outgoing request, e.g.
// "User-Agent" or correlation IDs. The SDK overrides "Authorization",
// "Accept", and "Content-Type" — those cannot be replaced.
func WithDefaultHeaders(h map[string]string) Option {
	return func(cfg *clientConfig) {
		cfg.defaultHeaders = make(map[string]string, len(h))
		for k, v := range h {
			cfg.defaultHeaders[k] = v
		}
	}
}

// NewClient constructs a Client.
//
// apiKey must be non-empty (the constructor returns a *ReroutError with code
// "missing_api_key" otherwise). All other settings have sensible defaults
// and can be overridden via Option arguments.
func NewClient(apiKey string, opts ...Option) (*Client, error) {
	if strings.TrimSpace(apiKey) == "" {
		return nil, &ReroutError{
			Code:    CodeMissingAPIKey,
			Message: "A project API key is required to construct rerout.Client.",
			Status:  0,
		}
	}

	cfg := clientConfig{
		baseURL: DefaultBaseURL,
		timeout: DefaultTimeout,
	}
	for _, opt := range opts {
		opt(&cfg)
	}

	httpClient := cfg.httpClient
	if httpClient == nil {
		timeout := cfg.timeout
		if !cfg.timeoutSet {
			timeout = DefaultTimeout
		}
		httpClient = &http.Client{Timeout: timeout}
	}

	c := &Client{
		apiKey:         apiKey,
		baseURL:        trimTrailingSlashes(cfg.baseURL),
		httpClient:     httpClient,
		defaultHeaders: cfg.defaultHeaders,
	}
	c.links = &Links{client: c}
	c.project = &ProjectNS{client: c}
	c.qr = &QR{client: c}
	c.webhooks = &Webhooks{client: c}
	c.conversions = &Conversions{client: c}
	return c, nil
}

// Links returns the link-operations namespace.
func (c *Client) Links() *Links { return c.links }

// Project returns the project-operations namespace.
func (c *Client) Project() *ProjectNS { return c.project }

// QR returns the QR-helpers namespace.
func (c *Client) QR() *QR { return c.qr }

// Webhooks returns the webhook-endpoint-management namespace.
func (c *Client) Webhooks() *Webhooks { return c.webhooks }

// Conversions returns the conversion-tracking namespace.
func (c *Client) Conversions() *Conversions { return c.conversions }

// BaseURL returns the resolved base URL — trailing-slash-trimmed. Exposed for
// diagnostics and the QR URL builder.
func (c *Client) BaseURL() string { return c.baseURL }

// ─── request transport ──────────────────────────────────────────────────

// requestOptions is the internal shape passed to Client.do.
type requestOptions struct {
	method string
	path   string
	query  url.Values
	body   any // nil ⇒ no body, no Content-Type
	// rawResponse toggles raw-string returns (used by QR.SVG).
	rawResponse bool
}

// do executes a single HTTP request, parses the response, and either decodes
// JSON into out or — when rawResponse is true — writes the body string into
// *outString. The function returns a *ReroutError for any failure.
func (c *Client) do(ctx context.Context, ro requestOptions, out any) error {
	u, err := url.Parse(c.baseURL + ro.path)
	if err != nil {
		return &ReroutError{
			Code:    CodeClientError,
			Status:  0,
			Message: fmt.Sprintf("failed to build URL: %s", err.Error()),
			Path:    ro.path,
			Cause:   err,
		}
	}
	if len(ro.query) > 0 {
		u.RawQuery = ro.query.Encode()
	}

	var bodyReader io.Reader
	var hasBody bool
	if ro.body != nil {
		buf, err := json.Marshal(ro.body)
		if err != nil {
			return &ReroutError{
				Code:    CodeClientError,
				Status:  0,
				Message: fmt.Sprintf("failed to encode JSON body: %s", err.Error()),
				Path:    ro.path,
				Cause:   err,
			}
		}
		bodyReader = bytes.NewReader(buf)
		hasBody = true
	}

	req, err := http.NewRequestWithContext(ctx, ro.method, u.String(), bodyReader)
	if err != nil {
		return &ReroutError{
			Code:    CodeClientError,
			Status:  0,
			Message: fmt.Sprintf("failed to build request: %s", err.Error()),
			Path:    ro.path,
			Cause:   err,
		}
	}

	// Default headers first so the SDK's own can overwrite them.
	for k, v := range c.defaultHeaders {
		req.Header.Set(k, v)
	}
	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	if ro.rawResponse {
		req.Header.Set("Accept", "image/svg+xml,text/plain,*/*")
	} else {
		req.Header.Set("Accept", "application/json")
	}
	if hasBody {
		req.Header.Set("Content-Type", "application/json")
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return classifyTransportError(err, ro.path)
	}
	defer func() { _ = resp.Body.Close() }()

	bodyBytes, readErr := io.ReadAll(resp.Body)
	if readErr != nil {
		return &ReroutError{
			Code:    CodeNetworkError,
			Status:  resp.StatusCode,
			Message: fmt.Sprintf("failed to read response body: %s", readErr.Error()),
			Path:    ro.path,
			Cause:   readErr,
		}
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return parseErrorResponse(resp.StatusCode, bodyBytes, ro.path)
	}

	if ro.rawResponse {
		if sp, ok := out.(*string); ok {
			*sp = string(bodyBytes)
			return nil
		}
		return &ReroutError{
			Code:    CodeClientError,
			Status:  resp.StatusCode,
			Message: "rerout: raw response requested but out is not *string",
			Path:    ro.path,
		}
	}

	if out == nil || len(bodyBytes) == 0 {
		return nil
	}

	if err := json.Unmarshal(bodyBytes, out); err != nil {
		return &ReroutError{
			Code:    CodeUnexpectedResponse,
			Status:  resp.StatusCode,
			Message: "Rerout returned a non-JSON success body.",
			Path:    ro.path,
			Details: map[string]any{"body": string(bodyBytes), "error": err.Error()},
			Cause:   err,
		}
	}
	return nil
}

// classifyTransportError converts an error from http.Client.Do into the right
// flavour of *ReroutError. The stdlib doesn't expose a typed timeout, so we
// sniff context.DeadlineExceeded and url.Error.Timeout().
func classifyTransportError(err error, path string) *ReroutError {
	if err == nil {
		return nil
	}
	if errors.Is(err, context.Canceled) {
		return &ReroutError{
			Code:    CodeNetworkError,
			Status:  0,
			Message: "request cancelled",
			Path:    path,
			Cause:   err,
		}
	}
	if errors.Is(err, context.DeadlineExceeded) {
		return &ReroutError{
			Code:    CodeTimeout,
			Status:  0,
			Message: "request timed out",
			Path:    path,
			Cause:   err,
		}
	}
	var urlErr *url.Error
	if errors.As(err, &urlErr) && urlErr.Timeout() {
		return &ReroutError{
			Code:    CodeTimeout,
			Status:  0,
			Message: "request timed out",
			Path:    path,
			Cause:   err,
		}
	}
	return &ReroutError{
		Code:    CodeNetworkError,
		Status:  0,
		Message: err.Error(),
		Path:    path,
		Cause:   err,
	}
}

// parseErrorResponse turns a non-2xx HTTP response into a *ReroutError.
//
// Order of precedence for Code/Message:
//  1. JSON body with `code` / `message` from the server.
//  2. synthetic code keyed off the status.
func parseErrorResponse(status int, body []byte, path string) *ReroutError {
	if len(body) == 0 {
		return &ReroutError{
			Code:    synthCodeForStatus(status),
			Status:  status,
			Message: fmt.Sprintf("Rerout returned HTTP %d with no body.", status),
			Path:    path,
		}
	}
	var parsed struct {
		Code      string `json:"code"`
		Message   string `json:"message"`
		Timestamp string `json:"timestamp"`
		Path      string `json:"path"`
	}
	if err := json.Unmarshal(body, &parsed); err != nil {
		return &ReroutError{
			Code:    synthCodeForStatus(status),
			Status:  status,
			Message: fmt.Sprintf("Rerout returned HTTP %d (non-JSON body).", status),
			Path:    path,
			Details: map[string]any{"body": string(body)},
		}
	}
	// Keep the full parsed map in Details for callers who want it.
	var detailsMap map[string]any
	_ = json.Unmarshal(body, &detailsMap)

	code := parsed.Code
	if code == "" {
		code = synthCodeForStatus(status)
	}
	msg := parsed.Message
	if msg == "" {
		msg = fmt.Sprintf("Rerout returned HTTP %d.", status)
	}
	resolvedPath := parsed.Path
	if resolvedPath == "" {
		resolvedPath = path
	}
	return &ReroutError{
		Code:      code,
		Status:    status,
		Message:   msg,
		Path:      resolvedPath,
		Timestamp: parsed.Timestamp,
		Details:   detailsMap,
	}
}

// trimTrailingSlashes strips every trailing '/' from s, mirroring the
// trailing-slash trimming the TS and Dart reference SDKs apply to baseUrl.
func trimTrailingSlashes(s string) string {
	for len(s) > 0 && s[len(s)-1] == '/' {
		s = s[:len(s)-1]
	}
	return s
}

// intToString and int64String render integers for cursor/limit/days query
// parameters. Tiny wrappers, but they keep the call sites in links.go and
// project.go terse.
func intToString(v int) string   { return strconv.Itoa(v) }
func int64String(v int64) string { return strconv.FormatInt(v, 10) }
