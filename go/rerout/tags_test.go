package rerout_test

import (
	"context"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── Tags.List ────────────────────────────────────────────────────────────

func TestTags_List_Success(t *testing.T) {
	t.Parallel()
	var method, path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"tags":[{"id":"tag_abc123","name":"Spring 2026","color":"teal","link_count":4}]}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Tags().List(context.Background())
	if err != nil {
		t.Fatalf("List: %v", err)
	}
	if method != http.MethodGet {
		t.Errorf("method = %q, want GET", method)
	}
	if path != "/v1/projects/me/tags" {
		t.Errorf("path = %q, want /v1/projects/me/tags", path)
	}
	if len(res.Tags) != 1 {
		t.Fatalf("len(Tags) = %d, want 1", len(res.Tags))
	}
	tag := res.Tags[0]
	if tag.ID != "tag_abc123" {
		t.Errorf("Tags[0].ID = %q, want tag_abc123", tag.ID)
	}
	if tag.Name != "Spring 2026" {
		t.Errorf("Tags[0].Name = %q, want Spring 2026", tag.Name)
	}
	if tag.Color != "teal" {
		t.Errorf("Tags[0].Color = %q, want teal", tag.Color)
	}
	if tag.LinkCount != 4 {
		t.Errorf("Tags[0].LinkCount = %d, want 4", tag.LinkCount)
	}
}

func TestTags_List_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Tags().List(context.Background())
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeUnauthorized {
		t.Fatalf("expected unauthorized ReroutError, got %v", err)
	}
}

// ─── Tags.Create ──────────────────────────────────────────────────────────

func TestTags_Create_Success(t *testing.T) {
	t.Parallel()
	var method, path, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"id":"tag_abc123","name":"Spring 2026","color":"teal"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Create(context.Background(), rerout.CreateTagInput{
		Name:  "Spring 2026",
		Color: rerout.String("teal"),
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	if method != http.MethodPost {
		t.Errorf("method = %q, want POST", method)
	}
	if path != "/v1/projects/me/tags" {
		t.Errorf("path = %q, want /v1/projects/me/tags", path)
	}
	if !strings.Contains(body, `"name":"Spring 2026"`) {
		t.Errorf("body missing name: %s", body)
	}
	if !strings.Contains(body, `"color":"teal"`) {
		t.Errorf("body missing color: %s", body)
	}
	if tag.ID != "tag_abc123" {
		t.Errorf("Tag.ID = %q, want tag_abc123", tag.ID)
	}
}

func TestTags_Create_OmitsColorWhenUnset(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"id":"tag_x","name":"No color","color":"teal"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Tags().Create(context.Background(), rerout.CreateTagInput{
		Name: "No color",
	}); err != nil {
		t.Fatalf("Create: %v", err)
	}
	if strings.Contains(body, "color") {
		t.Errorf("body should omit color when unset: %s", body)
	}
	if !strings.Contains(body, `"name":"No color"`) {
		t.Errorf("body missing name: %s", body)
	}
}

func TestTags_Create_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"code":"invalid_color","message":"color must be in the palette."}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Create(context.Background(), rerout.CreateTagInput{
		Name:  "Bad",
		Color: rerout.String("not-a-color"),
	})
	if tag != nil {
		t.Errorf("expected nil tag, got %v", tag)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != "invalid_color" {
		t.Errorf("Code = %q, want invalid_color", rerr.Code)
	}
	if rerr.Status != 400 {
		t.Errorf("Status = %d, want 400", rerr.Status)
	}
}

// ─── Tags.Update ──────────────────────────────────────────────────────────

func TestTags_Update_PatchesByIDAndEncodes(t *testing.T) {
	t.Parallel()
	var method, path, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"tag_abc123","name":"Spring 2026","color":"red"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Update(context.Background(), "tag_abc123", rerout.UpdateTagInput{
		Color: rerout.String("red"),
	})
	if err != nil {
		t.Fatalf("Update: %v", err)
	}
	if method != http.MethodPatch {
		t.Errorf("method = %q, want PATCH", method)
	}
	if path != "/v1/projects/me/tags/tag_abc123" {
		t.Errorf("path = %q, want /v1/projects/me/tags/tag_abc123", path)
	}
	// Only the provided field is forwarded.
	if strings.Contains(body, "name") {
		t.Errorf("body should omit name when unset: %s", body)
	}
	if !strings.Contains(body, `"color":"red"`) {
		t.Errorf("body missing color: %s", body)
	}
	if tag.Color != "red" {
		t.Errorf("Tag.Color = %q, want red", tag.Color)
	}
}

func TestTags_Update_ForwardsOnlyProvidedFields(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"tag_abc123","name":"Renamed","color":"teal"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Update(context.Background(), "tag_abc123", rerout.UpdateTagInput{
		Name: rerout.String("Renamed"),
	})
	if err != nil {
		t.Fatalf("Update: %v", err)
	}
	if strings.Contains(body, "color") {
		t.Errorf("body should omit color when unset: %s", body)
	}
	if !strings.Contains(body, `"name":"Renamed"`) {
		t.Errorf("body missing name: %s", body)
	}
	if tag.Name != "Renamed" {
		t.Errorf("Tag.Name = %q, want Renamed", tag.Name)
	}
}

func TestTags_Update_EncodesTagID(t *testing.T) {
	t.Parallel()
	var rawPath string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		rawPath = r.URL.EscapedPath()
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"id":"tag_a/b c","name":"x","color":"teal"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Tags().Update(context.Background(), "tag_a/b c", rerout.UpdateTagInput{
		Name: rerout.String("x"),
	}); err != nil {
		t.Fatalf("Update: %v", err)
	}
	if rawPath != "/v1/projects/me/tags/tag_a%2Fb%20c" {
		t.Errorf("escaped path = %q, want /v1/projects/me/tags/tag_a%%2Fb%%20c", rawPath)
	}
}

func TestTags_Update_EmptyInputIsClientError(t *testing.T) {
	t.Parallel()
	called := false
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		called = true
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Update(context.Background(), "tag_abc123", rerout.UpdateTagInput{})
	if tag != nil {
		t.Errorf("expected nil tag, got %v", tag)
	}
	if called {
		t.Error("empty UpdateTagInput should not hit the API")
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeBadRequest {
		t.Fatalf("expected bad_request ReroutError, got %v", err)
	}
}

func TestTags_Update_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
	})
	defer srv.Close()

	c := newClient(t, srv)
	tag, err := c.Tags().Update(context.Background(), "tag_missing", rerout.UpdateTagInput{
		Name: rerout.String("x"),
	})
	if tag != nil {
		t.Errorf("expected nil tag, got %v", tag)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeNotFound {
		t.Fatalf("expected not_found ReroutError, got %v", err)
	}
}

// ─── Tags.Delete ──────────────────────────────────────────────────────────

func TestTags_Delete_Success(t *testing.T) {
	t.Parallel()
	var method, path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"deleted":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Tags().Delete(context.Background(), "tag_abc123")
	if err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if method != http.MethodDelete {
		t.Errorf("method = %q, want DELETE", method)
	}
	if path != "/v1/projects/me/tags/tag_abc123" {
		t.Errorf("path = %q, want /v1/projects/me/tags/tag_abc123", path)
	}
	if !res.Deleted {
		t.Error("Deleted = false, want true")
	}
}

func TestTags_Delete_EncodesTagID(t *testing.T) {
	t.Parallel()
	var rawPath string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		rawPath = r.URL.EscapedPath()
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"deleted":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Tags().Delete(context.Background(), "tag_a/b c"); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rawPath != "/v1/projects/me/tags/tag_a%2Fb%20c" {
		t.Errorf("escaped path = %q, want /v1/projects/me/tags/tag_a%%2Fb%%20c", rawPath)
	}
}

func TestTags_Delete_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusForbidden)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Tags().Delete(context.Background(), "tag_abc123")
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeForbidden {
		t.Fatalf("expected forbidden ReroutError, got %v", err)
	}
}
