package rerout_test

import (
	"context"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── Links.CreateBatch ───────────────────────────────────────────────────

func TestLinks_CreateBatch_Success(t *testing.T) {
	t.Parallel()
	var method, path, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{
		  "created":2,
		  "total":3,
		  "results":[
		    {"index":0,"ok":true,"code":"abc"},
		    {"index":1,"ok":true,"code":"def"},
		    {"index":2,"ok":false,"error":"duplicate code"}
		  ]
		}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().CreateBatch(context.Background(), []rerout.BatchLinkInput{
		{TargetURL: "https://example.com/1"},
		{TargetURL: "https://example.com/2", Code: rerout.String("def"), DomainHostname: rerout.String("go.brand.com")},
		{TargetURL: "https://example.com/3", ExpiresAt: rerout.Int64(1800000000)},
	})
	if err != nil {
		t.Fatalf("CreateBatch: %v", err)
	}
	if method != http.MethodPost {
		t.Errorf("method = %q, want POST", method)
	}
	if path != "/v1/links/batch" {
		t.Errorf("path = %q, want /v1/links/batch", path)
	}
	if !strings.Contains(body, `"links":[`) {
		t.Errorf("body missing links wrapper: %s", body)
	}
	if !strings.Contains(body, `"domain_hostname":"go.brand.com"`) {
		t.Errorf("body missing domain_hostname: %s", body)
	}
	if res.Created != 2 || res.Total != 3 {
		t.Errorf("Created/Total = %d/%d, want 2/3", res.Created, res.Total)
	}
	if len(res.Results) != 3 {
		t.Fatalf("len(Results) = %d, want 3", len(res.Results))
	}
	if !res.Results[0].OK || res.Results[0].Code == nil || *res.Results[0].Code != "abc" {
		t.Errorf("Results[0] = %+v", res.Results[0])
	}
	if res.Results[2].OK || res.Results[2].Error == nil || *res.Results[2].Error != "duplicate code" {
		t.Errorf("Results[2] = %+v", res.Results[2])
	}
	if res.Results[2].Index != 2 {
		t.Errorf("Results[2].Index = %d, want 2", res.Results[2].Index)
	}
}

func TestLinks_CreateBatch_EmptyIsClientError(t *testing.T) {
	t.Parallel()
	called := false
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		called = true
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().CreateBatch(context.Background(), nil)
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeBadRequest {
		t.Fatalf("expected bad_request ReroutError, got %v", err)
	}
	if called {
		t.Error("server should not be called for empty batch")
	}
}

func TestLinks_CreateBatch_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusRequestEntityTooLarge)
		_, _ = w.Write([]byte(`{"code":"batch_too_large","message":"too many links."}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Links().CreateBatch(context.Background(), []rerout.BatchLinkInput{
		{TargetURL: "https://example.com/1"},
	})
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != "batch_too_large" {
		t.Fatalf("expected batch_too_large ReroutError, got %v", err)
	}
}
