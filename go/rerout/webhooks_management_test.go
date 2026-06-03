package rerout_test

import (
	"context"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// webhookJSON is a single endpoint object matching the SAMPLE_WEBHOOK in the
// TypeScript golden reference test.
const webhookJSON = `{
  "id":"wh_abc123",
  "project_id":"prj_test",
  "name":"Order events",
  "url":"https://example.com/hooks/rerout",
  "events":["link.created","link.clicked"],
  "is_active":true,
  "payload_format":"json",
  "created_at":1700000000,
  "updated_at":1700000000,
  "last_delivery_at":null,
  "last_success_at":null,
  "last_failure_at":null
}`

// ─── Webhooks.Create ──────────────────────────────────────────────────────

func TestWebhooks_Create_Success(t *testing.T) {
	t.Parallel()
	var method, path, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"endpoint":` + webhookJSON + `,"signing_secret":"whsec_supersecret"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().Create(context.Background(), rerout.CreateWebhookInput{
		Name:   "Order events",
		URL:    "https://example.com/hooks/rerout",
		Events: []string{"link.created", "link.clicked"},
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	if method != http.MethodPost {
		t.Errorf("method = %q, want POST", method)
	}
	if path != "/v1/projects/me/webhooks" {
		t.Errorf("path = %q, want /v1/projects/me/webhooks", path)
	}
	// Required fields present; optional ones omitted so the server applies defaults.
	if !strings.Contains(body, `"name":"Order events"`) {
		t.Errorf("body missing name: %s", body)
	}
	if !strings.Contains(body, `"url":"https://example.com/hooks/rerout"`) {
		t.Errorf("body missing url: %s", body)
	}
	if !strings.Contains(body, `"events":["link.created","link.clicked"]`) {
		t.Errorf("body missing events: %s", body)
	}
	if strings.Contains(body, "is_active") {
		t.Errorf("body should omit is_active when unset: %s", body)
	}
	if strings.Contains(body, "payload_format") {
		t.Errorf("body should omit payload_format when unset: %s", body)
	}
	if res.SigningSecret != "whsec_supersecret" {
		t.Errorf("SigningSecret = %q, want whsec_supersecret", res.SigningSecret)
	}
	if res.Endpoint.ID != "wh_abc123" {
		t.Errorf("Endpoint.ID = %q, want wh_abc123", res.Endpoint.ID)
	}
	if res.Endpoint.ProjectID != "prj_test" {
		t.Errorf("Endpoint.ProjectID = %q, want prj_test", res.Endpoint.ProjectID)
	}
	if len(res.Endpoint.Events) != 2 || res.Endpoint.Events[0] != "link.created" {
		t.Errorf("Endpoint.Events = %+v", res.Endpoint.Events)
	}
	if res.Endpoint.LastDeliveryAt != nil {
		t.Errorf("LastDeliveryAt = %v, want nil", res.Endpoint.LastDeliveryAt)
	}
}

func TestWebhooks_Create_ForwardsOptionalFields(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"endpoint":{"id":"wh_x","project_id":"prj_test","name":"Slack","url":"https://hooks.slack.com/services/T/B/x","events":["link.created"],"is_active":false,"payload_format":"slack","created_at":1,"updated_at":1,"last_delivery_at":null,"last_success_at":null,"last_failure_at":null},"signing_secret":"whsec_x"}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().Create(context.Background(), rerout.CreateWebhookInput{
		Name:          "Slack",
		URL:           "https://hooks.slack.com/services/T/B/x",
		Events:        []string{"link.created"},
		IsActive:      rerout.Bool(false),
		PayloadFormat: rerout.String("slack"),
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	if !strings.Contains(body, `"is_active":false`) {
		t.Errorf("body missing is_active=false: %s", body)
	}
	if !strings.Contains(body, `"payload_format":"slack"`) {
		t.Errorf("body missing payload_format=slack: %s", body)
	}
	if res.Endpoint.PayloadFormat != "slack" {
		t.Errorf("Endpoint.PayloadFormat = %q, want slack", res.Endpoint.PayloadFormat)
	}
	if res.Endpoint.IsActive {
		t.Error("Endpoint.IsActive = true, want false")
	}
}

func TestWebhooks_Create_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"code":"invalid_url","message":"url must be https."}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().Create(context.Background(), rerout.CreateWebhookInput{
		Name:   "Bad",
		URL:    "http://insecure.example",
		Events: []string{"link.created"},
	})
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil {
		t.Fatalf("expected ReroutError, got %T (%v)", err, err)
	}
	if rerr.Code != "invalid_url" {
		t.Errorf("Code = %q, want invalid_url", rerr.Code)
	}
	if rerr.Status != 400 {
		t.Errorf("Status = %d, want 400", rerr.Status)
	}
}

// ─── Webhooks.List ────────────────────────────────────────────────────────

func TestWebhooks_List_Success(t *testing.T) {
	t.Parallel()
	var method, path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"endpoints":[` + webhookJSON + `],"event_types":["link.created","link.clicked","domain.verified"]}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().List(context.Background())
	if err != nil {
		t.Fatalf("List: %v", err)
	}
	if method != http.MethodGet {
		t.Errorf("method = %q, want GET", method)
	}
	if path != "/v1/projects/me/webhooks" {
		t.Errorf("path = %q, want /v1/projects/me/webhooks", path)
	}
	if len(res.Endpoints) != 1 {
		t.Fatalf("len(Endpoints) = %d, want 1", len(res.Endpoints))
	}
	if res.Endpoints[0].URL != "https://example.com/hooks/rerout" {
		t.Errorf("Endpoints[0].URL = %q", res.Endpoints[0].URL)
	}
	found := false
	for _, et := range res.EventTypes {
		if et == "domain.verified" {
			found = true
		}
	}
	if !found {
		t.Errorf("EventTypes = %+v, want it to contain domain.verified", res.EventTypes)
	}
}

func TestWebhooks_List_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().List(context.Background())
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeUnauthorized {
		t.Fatalf("expected unauthorized ReroutError, got %v", err)
	}
}

// ─── Webhooks.Delete ──────────────────────────────────────────────────────

func TestWebhooks_Delete_Success(t *testing.T) {
	t.Parallel()
	var method, path string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"deleted":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().Delete(context.Background(), "wh_abc123")
	if err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if method != http.MethodDelete {
		t.Errorf("method = %q, want DELETE", method)
	}
	if path != "/v1/projects/me/webhooks/wh_abc123" {
		t.Errorf("path = %q, want /v1/projects/me/webhooks/wh_abc123", path)
	}
	if !res.Deleted {
		t.Error("Deleted = false, want true")
	}
}

func TestWebhooks_Delete_EncodesEndpointID(t *testing.T) {
	t.Parallel()
	var rawPath string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		rawPath = r.URL.EscapedPath()
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"deleted":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	if _, err := c.Webhooks().Delete(context.Background(), "wh_a/b c"); err != nil {
		t.Fatalf("Delete: %v", err)
	}
	if rawPath != "/v1/projects/me/webhooks/wh_a%2Fb%20c" {
		t.Errorf("escaped path = %q, want /v1/projects/me/webhooks/wh_a%%2Fb%%20c", rawPath)
	}
}

func TestWebhooks_Delete_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusForbidden)
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Webhooks().Delete(context.Background(), "wh_abc123")
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != rerout.CodeForbidden {
		t.Fatalf("expected forbidden ReroutError, got %v", err)
	}
}
