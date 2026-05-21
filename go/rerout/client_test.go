package rerout_test

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── construction ────────────────────────────────────────────────────────

func TestNewClient_RequiresAPIKey(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name string
		key  string
	}{
		{"empty", ""},
		{"whitespace", "   "},
		{"tab", "\t"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			c, err := rerout.NewClient(tc.key)
			if c != nil {
				t.Fatalf("expected nil client, got %v", c)
			}
			rerr := rerout.AsReroutError(err)
			if rerr == nil {
				t.Fatalf("expected *ReroutError, got %T (%v)", err, err)
			}
			if rerr.Code != rerout.CodeMissingAPIKey {
				t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeMissingAPIKey)
			}
			if rerr.Status != 0 {
				t.Errorf("Status = %d, want 0", rerr.Status)
			}
		})
	}
}

func TestNewClient_TrailingSlashTrimmed(t *testing.T) {
	t.Parallel()
	cases := []struct {
		in, want string
	}{
		{"https://api.rerout.co", "https://api.rerout.co"},
		{"https://api.rerout.co/", "https://api.rerout.co"},
		{"https://api.rerout.co////", "https://api.rerout.co"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.in, func(t *testing.T) {
			t.Parallel()
			c, err := rerout.NewClient("rrk_test", rerout.WithBaseURL(tc.in))
			if err != nil {
				t.Fatalf("NewClient: %v", err)
			}
			if got := c.BaseURL(); got != tc.want {
				t.Errorf("BaseURL() = %q, want %q", got, tc.want)
			}
		})
	}
}

func TestNewClient_NamespacesNonNil(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	if c.Links() == nil {
		t.Error("Links() is nil")
	}
	if c.Project() == nil {
		t.Error("Project() is nil")
	}
	if c.QR() == nil {
		t.Error("QR() is nil")
	}
}

func TestNewClient_DefaultBaseURL(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	if c.BaseURL() != rerout.DefaultBaseURL {
		t.Errorf("BaseURL() = %q, want %q", c.BaseURL(), rerout.DefaultBaseURL)
	}
}

func TestNewClient_WithHTTPClient(t *testing.T) {
	t.Parallel()
	// Verify the injected HTTP client is used: route to a server that
	// returns a known body and parse it.
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"prj_1","name":"acme","slug":"acme"}`))
	}))
	t.Cleanup(srv.Close)

	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL(srv.URL),
		rerout.WithHTTPClient(srv.Client()),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	got, err := c.Project().Me(context.Background())
	if err != nil {
		t.Fatalf("Project.Me: %v", err)
	}
	if got.ID != "prj_1" {
		t.Errorf("ID = %q, want prj_1", got.ID)
	}
}

// ─── request transport ──────────────────────────────────────────────────

func TestRequest_BearerAuthHeader(t *testing.T) {
	t.Parallel()
	var seen string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		seen = r.Header.Get("Authorization")
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"p","name":"n","slug":"s"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Project().Me(context.Background()); err != nil {
		t.Fatalf("Project.Me: %v", err)
	}
	if seen != "Bearer rrk_test" {
		t.Errorf("Authorization = %q, want %q", seen, "Bearer rrk_test")
	}
}

func TestRequest_ContentTypeOnlyWithBody(t *testing.T) {
	t.Parallel()
	var ctGET, ctPOST string

	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			ctGET = r.Header.Get("Content-Type")
			w.Header().Set("Content-Type", "application/json")
			_, _ = w.Write([]byte(`{"id":"p","name":"n","slug":"s"}`))
		case "POST":
			ctPOST = r.Header.Get("Content-Type")
			w.Header().Set("Content-Type", "application/json")
			_, _ = w.Write([]byte(linkJSON))
		}
	})
	defer srv.Close()

	c := newClient(t, srv)
	ctx := context.Background()
	if _, err := c.Project().Me(ctx); err != nil {
		t.Fatalf("Me: %v", err)
	}
	if _, err := c.Links().Create(ctx, rerout.CreateLinkInput{TargetURL: "https://example.com"}); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if ctGET != "" {
		t.Errorf("GET Content-Type = %q, want empty", ctGET)
	}
	if ctPOST != "application/json" {
		t.Errorf("POST Content-Type = %q, want application/json", ctPOST)
	}
}

func TestRequest_AcceptHeader(t *testing.T) {
	t.Parallel()
	var accept string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		accept = r.Header.Get("Accept")
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Links().Get(context.Background(), "abc"); err != nil {
		t.Fatalf("Get: %v", err)
	}
	if accept != "application/json" {
		t.Errorf("Accept = %q, want application/json", accept)
	}
}

func TestRequest_QueryParams(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name   string
		invoke func(*rerout.Client) error
		wantQ  string
	}{
		{
			name: "links list cursor+limit",
			invoke: func(c *rerout.Client) error {
				_, err := c.Links().List(context.Background(), &rerout.ListLinksParams{
					Cursor: rerout.Int64(42),
					Limit:  rerout.Int(10),
				})
				return err
			},
			wantQ: "cursor=42&limit=10",
		},
		{
			name: "links list cursor only",
			invoke: func(c *rerout.Client) error {
				_, err := c.Links().List(context.Background(), &rerout.ListLinksParams{
					Cursor: rerout.Int64(7),
				})
				return err
			},
			wantQ: "cursor=7",
		},
		{
			name: "links list nil params",
			invoke: func(c *rerout.Client) error {
				_, err := c.Links().List(context.Background(), nil)
				return err
			},
			wantQ: "",
		},
		{
			name: "links stats days",
			invoke: func(c *rerout.Client) error {
				_, err := c.Links().Stats(context.Background(), "abc", 7)
				return err
			},
			wantQ: "days=7",
		},
		{
			name: "links stats default days",
			invoke: func(c *rerout.Client) error {
				_, err := c.Links().Stats(context.Background(), "abc", 0)
				return err
			},
			wantQ: "days=30",
		},
		{
			name: "project stats days",
			invoke: func(c *rerout.Client) error {
				_, err := c.Project().Stats(context.Background(), 14)
				return err
			},
			wantQ: "days=14",
		},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			var seen string
			srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
				seen = r.URL.RawQuery
				w.Header().Set("Content-Type", "application/json")
				// Just return enough JSON to satisfy any of the typed
				// decoders. {} is enough for everything we exercise here.
				_, _ = w.Write([]byte(`{}`))
			})
			defer srv.Close()

			c := newClient(t, srv)
			if err := tc.invoke(c); err != nil {
				t.Fatalf("invoke: %v", err)
			}
			if seen != tc.wantQ {
				t.Errorf("RawQuery = %q, want %q", seen, tc.wantQ)
			}
		})
	}
}

func TestRequest_JSONBodySerialization(t *testing.T) {
	t.Parallel()
	var captured string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		buf := make([]byte, 1<<14)
		n, _ := r.Body.Read(buf)
		captured = string(buf[:n])
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Links().Create(context.Background(), rerout.CreateLinkInput{
		TargetURL:      "https://example.com",
		DomainHostname: rerout.String("go.brand.com"),
		Code:           rerout.String("q4"),
		ExpiresAt:      rerout.Int64(123),
		SEONoindex:     rerout.Bool(true),
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	for _, must := range []string{
		`"target_url":"https://example.com"`,
		`"domain_hostname":"go.brand.com"`,
		`"code":"q4"`,
		`"expires_at":123`,
		`"seo_noindex":true`,
	} {
		if !strings.Contains(captured, must) {
			t.Errorf("body missing %q\nfull body: %s", must, captured)
		}
	}
	// Omitted fields must not appear.
	for _, mustNot := range []string{
		`"seo_title"`, `"seo_description"`, `"seo_image_url"`, `"seo_canonical_url"`,
	} {
		if strings.Contains(captured, mustNot) {
			t.Errorf("body should omit %q\nfull body: %s", mustNot, captured)
		}
	}
}

func TestRequest_ErrorBodyParsed(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(400)
		_, _ = w.Write([]byte(`{"code":"bad_target_url","message":"target_url must use https.","timestamp":"2026-05-20T00:00:00Z","path":"/v1/links"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Links().Create(context.Background(), rerout.CreateLinkInput{TargetURL: "http://insecure"})
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != "bad_target_url" {
		t.Errorf("Code = %q, want bad_target_url", rerr.Code)
	}
	if rerr.Status != 400 {
		t.Errorf("Status = %d, want 400", rerr.Status)
	}
	if !strings.Contains(rerr.Message, "must use https") {
		t.Errorf("Message = %q", rerr.Message)
	}
	if rerr.Timestamp == "" {
		t.Error("Timestamp empty, want server-supplied")
	}
}

func TestRequest_SyntheticCodesForStatuses(t *testing.T) {
	t.Parallel()
	cases := []struct {
		status   int
		wantCode string
		flag     func(*rerout.ReroutError) bool
	}{
		{401, rerout.CodeUnauthorized, nil},
		{403, rerout.CodeForbidden, nil},
		{404, rerout.CodeNotFound, nil},
		{429, rerout.CodeRateLimited, (*rerout.ReroutError).IsRateLimited},
		{500, rerout.CodeServerError, (*rerout.ReroutError).IsServerError},
		{502, rerout.CodeServerError, (*rerout.ReroutError).IsServerError},
		{503, rerout.CodeServerError, (*rerout.ReroutError).IsServerError},
		{418, rerout.CodeClientError, nil},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(http.StatusText(tc.status), func(t *testing.T) {
			t.Parallel()
			srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tc.status)
				// No body — exercise synthetic-code path.
			})
			defer srv.Close()

			c := newClient(t, srv)
			_, err := c.Links().Get(context.Background(), "abc")
			rerr := rerout.AsReroutError(err)
			if rerr == nil {
				t.Fatalf("expected ReroutError, got %T (%v)", err, err)
			}
			if rerr.Code != tc.wantCode {
				t.Errorf("Code = %q, want %q", rerr.Code, tc.wantCode)
			}
			if rerr.Status != tc.status {
				t.Errorf("Status = %d, want %d", rerr.Status, tc.status)
			}
			if tc.flag != nil && !tc.flag(rerr) {
				t.Errorf("flag check failed for status %d", tc.status)
			}
		})
	}
}

func TestRequest_NetworkErrorCode(t *testing.T) {
	t.Parallel()
	// Point the client at an address that won't accept connections.
	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL("http://127.0.0.1:1"),
		rerout.WithTimeout(2*time.Second),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	_, err = c.Project().Me(context.Background())
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != rerout.CodeNetworkError {
		t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeNetworkError)
	}
	if rerr.Status != 0 {
		t.Errorf("Status = %d, want 0", rerr.Status)
	}
}

func TestRequest_TimeoutCode(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(200 * time.Millisecond)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{}`))
	})
	defer srv.Close()

	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL(srv.URL),
		rerout.WithTimeout(50*time.Millisecond),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	_, err = c.Project().Me(context.Background())
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != rerout.CodeTimeout {
		t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeTimeout)
	}
}

func TestRequest_ContextDeadlineMapsToTimeout(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(200 * time.Millisecond)
		_, _ = w.Write([]byte(`{}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	ctx, cancel := context.WithTimeout(context.Background(), 25*time.Millisecond)
	defer cancel()
	_, err := c.Project().Me(ctx)
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != rerout.CodeTimeout {
		t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeTimeout)
	}
}

func TestRequest_UnexpectedResponseOn2xxNonJSON(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		_, _ = w.Write([]byte("not json at all"))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Project().Me(context.Background())
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != rerout.CodeUnexpectedResponse {
		t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeUnexpectedResponse)
	}
}

func TestRequest_DefaultHeadersForwarded(t *testing.T) {
	t.Parallel()
	var seen string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		seen = r.Header.Get("User-Agent")
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"p","name":"n","slug":"s"}`))
	})
	defer srv.Close()

	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL(srv.URL),
		rerout.WithDefaultHeaders(map[string]string{"User-Agent": "rerout-go-test/1.0"}),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	if _, err := c.Project().Me(context.Background()); err != nil {
		t.Fatalf("Me: %v", err)
	}
	if seen != "rerout-go-test/1.0" {
		t.Errorf("User-Agent = %q", seen)
	}
}

func TestRequest_AuthorizationCannotBeOverridden(t *testing.T) {
	t.Parallel()
	// Default headers must not let callers replace the SDK's own auth.
	var seenAuth string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		seenAuth = r.Header.Get("Authorization")
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"p","name":"n","slug":"s"}`))
	})
	defer srv.Close()

	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL(srv.URL),
		rerout.WithDefaultHeaders(map[string]string{"Authorization": "Bearer hijack"}),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	if _, err := c.Project().Me(context.Background()); err != nil {
		t.Fatalf("Me: %v", err)
	}
	if seenAuth != "Bearer rrk_test" {
		t.Errorf("Authorization = %q, want Bearer rrk_test", seenAuth)
	}
}

func TestReroutError_UnwrapAndErrorsAs(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(429)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"code":"rate_limited","message":"slow down"}`))
	})
	defer srv.Close()
	c := newClient(t, srv)
	_, err := c.Links().Get(context.Background(), "abc")

	var rerr *rerout.ReroutError
	if !errors.As(err, &rerr) {
		t.Fatalf("errors.As failed: %v", err)
	}
	if !rerr.IsRateLimited() {
		t.Error("IsRateLimited() = false, want true")
	}
	if rerr.IsServerError() {
		t.Error("IsServerError() = true, want false")
	}
}

// ─── helpers ────────────────────────────────────────────────────────────

const linkJSON = `{
  "code":"q4",
  "short_url":"https://go.brand.com/q4",
  "domain_hostname":"go.brand.com",
  "target_url":"https://example.com/q4-sale",
  "project_id":"prj_1",
  "is_active":true,
  "seo_noindex":false,
  "created_at":1700000000,
  "updated_at":1700000100
}`

func newClient(t *testing.T, srv *httptest.Server) *rerout.Client {
	t.Helper()
	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL(srv.URL),
		rerout.WithHTTPClient(srv.Client()),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	return c
}

func newTestServer(h http.HandlerFunc) *httptest.Server {
	return httptest.NewServer(h)
}
