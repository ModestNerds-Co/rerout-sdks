package rerout_test

import (
	"context"
	"io"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── Links.Create ────────────────────────────────────────────────────────

func TestLinks_Create_Success(t *testing.T) {
	t.Parallel()
	var method, path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Create(context.Background(), rerout.CreateLinkInput{
		TargetURL: "https://example.com/q4-sale",
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	if method != http.MethodPost {
		t.Errorf("method = %q, want POST", method)
	}
	if path != "/v1/links" {
		t.Errorf("path = %q, want /v1/links", path)
	}
	if link.Code != "q4" {
		t.Errorf("Code = %q, want q4", link.Code)
	}
	if link.ShortURL != "https://go.brand.com/q4" {
		t.Errorf("ShortURL = %q", link.ShortURL)
	}
	if link.DomainHostname == nil || *link.DomainHostname != "go.brand.com" {
		t.Errorf("DomainHostname = %v, want go.brand.com", link.DomainHostname)
	}
	if link.IsActive != true {
		t.Errorf("IsActive = %v, want true", link.IsActive)
	}
}

func TestLinks_Create_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"code":"bad_target_url","message":"target_url must use https."}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Create(context.Background(), rerout.CreateLinkInput{
		TargetURL: "http://insecure.example",
	})
	if link != nil {
		t.Errorf("expected nil link, got %v", link)
	}
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
}

// ─── Links.List ──────────────────────────────────────────────────────────

func TestLinks_List_Success(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{
		  "links":[
		    {"code":"a","short_url":"https://rerout.co/a","target_url":"https://x.com","project_id":"p","is_active":true,"seo_noindex":false,"created_at":1,"updated_at":2},
		    {"code":"b","short_url":"https://rerout.co/b","target_url":"https://y.com","project_id":"p","is_active":false,"seo_noindex":true,"created_at":3,"updated_at":4}
		  ],
		  "next_cursor":99
		}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().List(context.Background(), nil)
	if err != nil {
		t.Fatalf("List: %v", err)
	}
	if len(res.Links) != 2 {
		t.Fatalf("len(Links) = %d, want 2", len(res.Links))
	}
	if res.Links[0].Code != "a" || res.Links[1].Code != "b" {
		t.Errorf("codes = %q,%q", res.Links[0].Code, res.Links[1].Code)
	}
	if !res.HasMore() {
		t.Error("HasMore() = false, want true")
	}
	if res.NextCursor == nil || *res.NextCursor != 99 {
		t.Errorf("NextCursor = %v, want 99", res.NextCursor)
	}
}

func TestLinks_List_LastPageHasNoMore(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"links":[],"next_cursor":null}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().List(context.Background(), &rerout.ListLinksParams{
		Limit: rerout.Int(50),
	})
	if err != nil {
		t.Fatalf("List: %v", err)
	}
	if res.HasMore() {
		t.Error("HasMore() = true, want false")
	}
	if res.NextCursor != nil {
		t.Errorf("NextCursor = %v, want nil", res.NextCursor)
	}
}

func TestLinks_List_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().List(context.Background(), nil)
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeUnauthorized {
		t.Fatalf("expected unauthorized ReroutError, got %v", err)
	}
}

// ─── Links.Get ───────────────────────────────────────────────────────────

func TestLinks_Get_Success(t *testing.T) {
	t.Parallel()
	var path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		path = r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Get(context.Background(), "q4")
	if err != nil {
		t.Fatalf("Get: %v", err)
	}
	if path != "/v1/links/q4" {
		t.Errorf("path = %q, want /v1/links/q4", path)
	}
	if link.Code != "q4" {
		t.Errorf("Code = %q, want q4", link.Code)
	}
}

func TestLinks_Get_NotFound(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte(`{"code":"not_found","message":"no such link"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Get(context.Background(), "missing")
	if link != nil {
		t.Errorf("expected nil link, got %v", link)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeNotFound {
		t.Fatalf("expected not_found ReroutError, got %v", err)
	}
	if rerr.Status != 404 {
		t.Errorf("Status = %d, want 404", rerr.Status)
	}
}

// ─── Links.Update ────────────────────────────────────────────────────────

func TestLinks_Update_Success(t *testing.T) {
	t.Parallel()
	var method, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method = r.Method
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Update(context.Background(), "q4", rerout.UpdateLinkInput{
		TargetURL: rerout.String("https://example.com/new"),
		IsActive:  rerout.Bool(false),
	})
	if err != nil {
		t.Fatalf("Update: %v", err)
	}
	if method != http.MethodPatch {
		t.Errorf("method = %q, want PATCH", method)
	}
	if link.Code != "q4" {
		t.Errorf("Code = %q, want q4", link.Code)
	}
	if !strings.Contains(body, `"target_url":"https://example.com/new"`) {
		t.Errorf("body missing target_url: %s", body)
	}
	if !strings.Contains(body, `"is_active":false`) {
		t.Errorf("body missing is_active: %s", body)
	}
}

func TestLinks_Update_EmptyInputRejectedClientSide(t *testing.T) {
	t.Parallel()
	hit := false
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		hit = true
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Update(context.Background(), "q4", rerout.UpdateLinkInput{})
	if hit {
		t.Error("empty UpdateLinkInput hit the API; it must be rejected client-side")
	}
	if link != nil {
		t.Errorf("expected nil link, got %v", link)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != rerout.CodeBadRequest {
		t.Errorf("Code = %q, want %q", rerr.Code, rerout.CodeBadRequest)
	}
	if rerr.Status != 0 {
		t.Errorf("Status = %d, want 0", rerr.Status)
	}
}

func TestLinks_Update_ClearFieldsSendExplicitNull(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Links().Update(context.Background(), "q4", rerout.UpdateLinkInput{
		ClearExpiresAt:   true,
		ClearSEOImageURL: true,
	})
	if err != nil {
		t.Fatalf("Update: %v", err)
	}
	if !strings.Contains(body, `"expires_at":null`) {
		t.Errorf("body missing explicit null expires_at: %s", body)
	}
	if !strings.Contains(body, `"seo_image_url":null`) {
		t.Errorf("body missing explicit null seo_image_url: %s", body)
	}
}

func TestLinks_Update_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte(`{"code":"not_found","message":"no such link"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Links().Update(context.Background(), "missing", rerout.UpdateLinkInput{
		IsActive: rerout.Bool(true),
	})
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeNotFound {
		t.Fatalf("expected not_found ReroutError, got %v", err)
	}
}

// ─── Links.Delete ────────────────────────────────────────────────────────

func TestLinks_Delete_Success(t *testing.T) {
	t.Parallel()
	var method string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method = r.Method
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"deleted":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().Delete(context.Background(), "q4")
	if err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if method != http.MethodDelete {
		t.Errorf("method = %q, want DELETE", method)
	}
	if !res.Deleted {
		t.Error("Deleted = false, want true")
	}
}

func TestLinks_Delete_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusForbidden)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().Delete(context.Background(), "q4")
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeForbidden {
		t.Fatalf("expected forbidden ReroutError, got %v", err)
	}
}

// ─── Links.Stats ─────────────────────────────────────────────────────────

func TestLinks_Stats_Success(t *testing.T) {
	t.Parallel()
	var path, query string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		path, query = r.URL.Path, r.URL.RawQuery
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{
		  "code":"q4","days":7,"total_clicks":123,"qr_scans":45,
		  "countries":[{"value":"ZA","clicks":80},{"value":"US","clicks":43}],
		  "referrers":[{"value":"twitter.com","clicks":50}]
		}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	stats, err := c.Links().Stats(context.Background(), "q4", 7)
	if err != nil {
		t.Fatalf("Stats: %v", err)
	}
	if path != "/v1/links/q4/stats" {
		t.Errorf("path = %q, want /v1/links/q4/stats", path)
	}
	if query != "days=7" {
		t.Errorf("query = %q, want days=7", query)
	}
	if stats.TotalClicks != 123 {
		t.Errorf("TotalClicks = %d, want 123", stats.TotalClicks)
	}
	if stats.QRScans != 45 {
		t.Errorf("QRScans = %d, want 45", stats.QRScans)
	}
	if len(stats.Countries) != 2 || stats.Countries[0].Value != "ZA" {
		t.Errorf("Countries = %+v", stats.Countries)
	}
}

func TestLinks_Stats_DefaultsTo30Days(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name string
		days int
		want string
	}{
		{"zero", 0, "days=30"},
		{"negative", -5, "days=30"},
		{"explicit", 14, "days=14"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			var query string
			srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
				query = r.URL.RawQuery
				w.Header().Set("Content-Type", "application/json")
				_, _ = w.Write([]byte(`{}`))
			})
			defer srv.Close()

			c := newClient(t, srv)
			if _, err := c.Links().Stats(context.Background(), "q4", tc.days); err != nil {
				t.Fatalf("Stats: %v", err)
			}
			if query != tc.want {
				t.Errorf("query = %q, want %q", query, tc.want)
			}
		})
	}
}

func TestLinks_Stats_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	})
	defer srv.Close()

	c := newClient(t, srv)
	stats, err := c.Links().Stats(context.Background(), "q4", 30)
	if stats != nil {
		t.Errorf("expected nil stats, got %v", stats)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeServerError {
		t.Fatalf("expected server_error ReroutError, got %v", err)
	}
	if !rerr.IsServerError() {
		t.Error("IsServerError() = false, want true")
	}
}

// ─── Project namespace ───────────────────────────────────────────────────

func TestProject_Stats_Success(t *testing.T) {
	t.Parallel()
	var path, query string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		path, query = r.URL.Path, r.URL.RawQuery
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{
		  "days":30,"total_clicks":5000,"qr_scans":900,
		  "daily":[{"day":1700000000,"clicks":100,"qr_scans":10}],
		  "countries":[{"value":"ZA","clicks":3000}],
		  "referrers":[{"value":"direct","clicks":2000}],
		  "devices":[{"value":"mobile","clicks":3500}],
		  "browsers":[{"value":"chrome","clicks":2800}],
		  "top_codes":[{"value":"q4","clicks":1200}]
		}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	stats, err := c.Project().Stats(context.Background(), 30)
	if err != nil {
		t.Fatalf("Project.Stats: %v", err)
	}
	if path != "/v1/projects/me/stats" {
		t.Errorf("path = %q, want /v1/projects/me/stats", path)
	}
	if query != "days=30" {
		t.Errorf("query = %q, want days=30", query)
	}
	if stats.TotalClicks != 5000 {
		t.Errorf("TotalClicks = %d, want 5000", stats.TotalClicks)
	}
	if len(stats.Daily) != 1 || stats.Daily[0].Clicks != 100 {
		t.Errorf("Daily = %+v", stats.Daily)
	}
	if len(stats.TopCodes) != 1 || stats.TopCodes[0].Value != "q4" {
		t.Errorf("TopCodes = %+v", stats.TopCodes)
	}
}

func TestProject_Stats_DefaultsTo30Days(t *testing.T) {
	t.Parallel()
	var query string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		query = r.URL.RawQuery
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Project().Stats(context.Background(), 0); err != nil {
		t.Fatalf("Project.Stats: %v", err)
	}
	if query != "days=30" {
		t.Errorf("query = %q, want days=30", query)
	}
}

func TestProject_Stats_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusTooManyRequests)
	})
	defer srv.Close()

	c := newClient(t, srv)
	stats, err := c.Project().Stats(context.Background(), 30)
	if stats != nil {
		t.Errorf("expected nil stats, got %v", stats)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeRateLimited {
		t.Fatalf("expected rate_limited ReroutError, got %v", err)
	}
	if !rerr.IsRateLimited() {
		t.Error("IsRateLimited() = false, want true")
	}
}

func TestProject_Me_Success(t *testing.T) {
	t.Parallel()
	var path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		path = r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"prj_42","name":"Acme Co","slug":"acme"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	proj, err := c.Project().Me(context.Background())
	if err != nil {
		t.Fatalf("Project.Me: %v", err)
	}
	if path != "/v1/projects/me" {
		t.Errorf("path = %q, want /v1/projects/me", path)
	}
	if proj.ID != "prj_42" || proj.Name != "Acme Co" || proj.Slug != "acme" {
		t.Errorf("proj = %+v", proj)
	}
}

func TestProject_Me_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte(`{"code":"unauthorized","message":"bad api key"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	proj, err := c.Project().Me(context.Background())
	if proj != nil {
		t.Errorf("expected nil project, got %v", proj)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeUnauthorized {
		t.Fatalf("expected unauthorized ReroutError, got %v", err)
	}
}

// ─── URL encoding edge cases ─────────────────────────────────────────────

func TestLinks_CodeURLEncoding(t *testing.T) {
	t.Parallel()
	// url.PathEscape percent-encodes spaces, non-ASCII, '/', '?', '#' and '%'
	// so the code is always a single path segment; '+' is left as-is. What
	// the server receives in r.URL.Path is the *decoded* segment, so we
	// assert on both the escaped and decoded paths to prove the round-trip
	// is lossless.
	cases := []struct {
		name     string
		code     string
		wantRaw  string // raw (escaped) path the client must send
		wantPath string // decoded path the server should see
	}{
		{"space", "hello world", "/v1/links/hello%20world", "/v1/links/hello world"},
		{"plus", "a+b", "/v1/links/a+b", "/v1/links/a+b"},
		{"unicode", "café", "/v1/links/caf%C3%A9", "/v1/links/café"},
		{"slash", "go/promo", "/v1/links/go%2Fpromo", "/v1/links/go/promo"},
		{"percent", "a%b", "/v1/links/a%25b", "/v1/links/a%b"},
		{"question", "a?b", "/v1/links/a%3Fb", "/v1/links/a?b"},
		{"hash", "a#b", "/v1/links/a%23b", "/v1/links/a#b"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			var rawPath, decodedPath string
			srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
				rawPath = r.URL.EscapedPath()
				decodedPath = r.URL.Path
				w.Header().Set("Content-Type", "application/json")
				_, _ = w.Write([]byte(linkJSON))
			})
			defer srv.Close()

			c := newClient(t, srv)
			if _, err := c.Links().Get(context.Background(), tc.code); err != nil {
				t.Fatalf("Get(%q): %v", tc.code, err)
			}
			if rawPath != tc.wantRaw {
				t.Errorf("escaped path = %q, want %q", rawPath, tc.wantRaw)
			}
			if decodedPath != tc.wantPath {
				t.Errorf("decoded path = %q, want %q", decodedPath, tc.wantPath)
			}
		})
	}
}

// readBody drains an httptest request body into a string. The handler is the
// only reader, so a single ReadAll is safe.
func readBody(r *http.Request) string {
	if r.Body == nil {
		return ""
	}
	b, _ := io.ReadAll(r.Body)
	return string(b)
}
