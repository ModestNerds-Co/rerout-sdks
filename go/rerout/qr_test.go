package rerout_test

import (
	"context"
	"net/http"
	"net/url"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── QR.URL — pure builder ───────────────────────────────────────────────

func TestQR_URL_BareNoOptions(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	got := c.QR().URL("q4", nil)
	want := "https://api.rerout.co/v1/links/q4/qr"
	if got != want {
		t.Errorf("URL = %q, want %q", got, want)
	}
}

func TestQR_URL_EmptyOptionsBag(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	// A non-nil but empty *QROptions must still produce a bare URL.
	got := c.QR().URL("q4", &rerout.QROptions{})
	want := "https://api.rerout.co/v1/links/q4/qr"
	if got != want {
		t.Errorf("URL = %q, want %q", got, want)
	}
}

func TestQR_URL_EveryOptionEmitted(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	got := c.QR().URL("q4", &rerout.QROptions{
		Size:        rerout.Int(12),
		Margin:      rerout.Int(2),
		ECC:         "H",
		Domain:      "go.brand.com",
		RefreshTrue: true,
	})
	u, err := url.Parse(got)
	if err != nil {
		t.Fatalf("parse %q: %v", got, err)
	}
	if u.Path != "/v1/links/q4/qr" {
		t.Errorf("path = %q, want /v1/links/q4/qr", u.Path)
	}
	q := u.Query()
	checks := map[string]string{
		"size":    "12",
		"margin":  "2",
		"ecc":     "H",
		"domain":  "go.brand.com",
		"refresh": "1",
	}
	for k, want := range checks {
		if got := q.Get(k); got != want {
			t.Errorf("query %q = %q, want %q", k, got, want)
		}
	}
}

func TestQR_URL_OmitsUnsetOptions(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	got := c.QR().URL("q4", &rerout.QROptions{Size: rerout.Int(8)})
	want := "https://api.rerout.co/v1/links/q4/qr?size=8"
	if got != want {
		t.Errorf("URL = %q, want %q", got, want)
	}
}

func TestQR_URL_DeterministicOrdering(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	opts := &rerout.QROptions{
		Size:   rerout.Int(8),
		Margin: rerout.Int(4),
		ECC:    "M",
	}
	// The builder sorts query keys, so repeated calls must be byte-identical.
	first := c.QR().URL("q4", opts)
	for i := 0; i < 20; i++ {
		if got := c.QR().URL("q4", opts); got != first {
			t.Fatalf("non-deterministic URL: %q != %q", got, first)
		}
	}
	want := "https://api.rerout.co/v1/links/q4/qr?ecc=M&margin=4&size=8"
	if first != want {
		t.Errorf("URL = %q, want %q", first, want)
	}
}

func TestQR_URL_CustomBaseURLHonoured(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test",
		rerout.WithBaseURL("https://api.staging.example.com/"),
	)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	got := c.QR().URL("q4", nil)
	want := "https://api.staging.example.com/v1/links/q4/qr"
	if got != want {
		t.Errorf("URL = %q, want %q", got, want)
	}
}

func TestQR_URL_RefreshBehaviour(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	cases := []struct {
		name string
		opts *rerout.QROptions
		want string // expected refresh query value; "" means refresh absent
	}{
		{"refresh true → 1", &rerout.QROptions{RefreshTrue: true}, "1"},
		{"refresh string v2", &rerout.QROptions{Refresh: "v2"}, "v2"},
		{"refresh true wins over string", &rerout.QROptions{RefreshTrue: true, Refresh: "v2"}, "1"},
		{"refresh unset", &rerout.QROptions{}, ""},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			u, err := url.Parse(c.QR().URL("q4", tc.opts))
			if err != nil {
				t.Fatalf("parse: %v", err)
			}
			got := u.Query().Get("refresh")
			if got != tc.want {
				t.Errorf("refresh = %q, want %q", got, tc.want)
			}
		})
	}
}

func TestQR_URL_CodeEncoding(t *testing.T) {
	t.Parallel()
	c, err := rerout.NewClient("rrk_test")
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	// url.PathEscape encodes a slash to %2F so the code stays one path
	// segment; spaces and non-ASCII are percent-encoded; '+' is left as-is.
	cases := []struct {
		name, code, want string
	}{
		{"space", "hello world", "https://api.rerout.co/v1/links/hello%20world/qr"},
		{"unicode", "café", "https://api.rerout.co/v1/links/caf%C3%A9/qr"},
		{"plus", "a+b", "https://api.rerout.co/v1/links/a+b/qr"},
		{"slash", "go/promo", "https://api.rerout.co/v1/links/go%2Fpromo/qr"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := c.QR().URL(tc.code, nil); got != tc.want {
				t.Errorf("URL = %q, want %q", got, tc.want)
			}
		})
	}
}

// ─── BuildQRURL — standalone builder ─────────────────────────────────────

func TestBuildQRURL_Standalone(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name    string
		baseURL string
		code    string
		opts    *rerout.QROptions
		want    string
	}{
		{
			name:    "bare",
			baseURL: "https://api.rerout.co",
			code:    "q4",
			want:    "https://api.rerout.co/v1/links/q4/qr",
		},
		{
			name:    "trailing slashes trimmed",
			baseURL: "https://api.rerout.co///",
			code:    "q4",
			want:    "https://api.rerout.co/v1/links/q4/qr",
		},
		{
			name:    "with single option",
			baseURL: "https://api.rerout.co",
			code:    "q4",
			opts:    &rerout.QROptions{ECC: "Q"},
			want:    "https://api.rerout.co/v1/links/q4/qr?ecc=Q",
		},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := rerout.BuildQRURL(tc.baseURL, tc.code, tc.opts); got != tc.want {
				t.Errorf("BuildQRURL = %q, want %q", got, tc.want)
			}
		})
	}
}

// ─── QR.SVG — authenticated fetch ────────────────────────────────────────

func TestQR_SVG_Success(t *testing.T) {
	t.Parallel()
	const svgBody = `<svg xmlns="http://www.w3.org/2000/svg"><rect/></svg>`
	var path, auth, query string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		path, auth, query = r.URL.Path, r.Header.Get("Authorization"), r.URL.RawQuery
		w.Header().Set("Content-Type", "image/svg+xml")
		_, _ = w.Write([]byte(svgBody))
	})
	defer srv.Close()

	c := newClient(t, srv)
	got, err := c.QR().SVG(context.Background(), "q4", &rerout.QROptions{
		Size: rerout.Int(10),
		ECC:  "H",
	})
	if err != nil {
		t.Fatalf("SVG: %v", err)
	}
	if got != svgBody {
		t.Errorf("SVG body = %q, want %q", got, svgBody)
	}
	if path != "/v1/links/q4/qr" {
		t.Errorf("path = %q, want /v1/links/q4/qr", path)
	}
	if auth != "Bearer rrk_test" {
		t.Errorf("Authorization = %q, want Bearer rrk_test", auth)
	}
	if !strings.Contains(query, "size=10") || !strings.Contains(query, "ecc=H") {
		t.Errorf("query = %q, want size=10 & ecc=H", query)
	}
}

func TestQR_SVG_NoOptions(t *testing.T) {
	t.Parallel()
	var query string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		query = r.URL.RawQuery
		w.Header().Set("Content-Type", "image/svg+xml")
		_, _ = w.Write([]byte(`<svg/>`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	got, err := c.QR().SVG(context.Background(), "q4", nil)
	if err != nil {
		t.Fatalf("SVG: %v", err)
	}
	if got != `<svg/>` {
		t.Errorf("SVG body = %q", got)
	}
	if query != "" {
		t.Errorf("query = %q, want empty", query)
	}
}

func TestQR_SVG_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte(`{"code":"not_found","message":"no such link"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	got, err := c.QR().SVG(context.Background(), "missing", nil)
	if got != "" {
		t.Errorf("expected empty SVG on error, got %q", got)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeNotFound {
		t.Fatalf("expected not_found ReroutError, got %v", err)
	}
}
