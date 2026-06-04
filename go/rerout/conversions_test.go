package rerout_test

import (
	"context"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── Conversions.Record ──────────────────────────────────────────────────

func TestConversions_Record_Success(t *testing.T) {
	t.Parallel()
	var method, path, body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		method, path = r.Method, r.URL.Path
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"recorded":true,"duplicate":false}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Conversions().Record(context.Background(), rerout.RecordConversionInput{
		ClickID:    "clk_123",
		EventName:  "purchase",
		ValueCents: rerout.Int64(4999),
		Currency:   rerout.String("USD"),
	})
	if err != nil {
		t.Fatalf("Record: %v", err)
	}
	if method != http.MethodPost {
		t.Errorf("method = %q, want POST", method)
	}
	if path != "/v1/conversions" {
		t.Errorf("path = %q, want /v1/conversions", path)
	}
	for _, want := range []string{
		`"click_id":"clk_123"`,
		`"event_name":"purchase"`,
		`"value_cents":4999`,
		`"currency":"USD"`,
	} {
		if !strings.Contains(body, want) {
			t.Errorf("body missing %s: %s", want, body)
		}
	}
	if !res.Recorded {
		t.Error("Recorded = false, want true")
	}
	if res.Duplicate {
		t.Error("Duplicate = true, want false")
	}
}

func TestConversions_Record_OmitsOptionalFields(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"recorded":true,"duplicate":false}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Conversions().Record(context.Background(), rerout.RecordConversionInput{
		ClickID:   "clk_1",
		EventName: "signup",
	})
	if err != nil {
		t.Fatalf("Record: %v", err)
	}
	if strings.Contains(body, "value_cents") {
		t.Errorf("body should omit value_cents: %s", body)
	}
	if strings.Contains(body, "currency") {
		t.Errorf("body should omit currency: %s", body)
	}
}

func TestConversions_Record_Duplicate(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"recorded":false,"duplicate":true}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Conversions().Record(context.Background(), rerout.RecordConversionInput{
		ClickID:   "clk_1",
		EventName: "purchase",
	})
	if err != nil {
		t.Fatalf("Record: %v", err)
	}
	if res.Recorded {
		t.Error("Recorded = true, want false")
	}
	if !res.Duplicate {
		t.Error("Duplicate = false, want true")
	}
}

func TestConversions_Record_Error(t *testing.T) {
	t.Parallel()
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte(`{"code":"click_not_found","message":"no such click."}`))
	})
	defer srv.Close()

	c := newClient(t, srv)
	res, err := c.Conversions().Record(context.Background(), rerout.RecordConversionInput{
		ClickID:   "clk_missing",
		EventName: "purchase",
	})
	if res != nil {
		t.Errorf("expected nil result, got %v", res)
	}
	rerr := rerout.AsReroutError(err)
	if rerr == nil || rerr.Code != "click_not_found" {
		t.Fatalf("expected click_not_found ReroutError, got %v", err)
	}
	if rerr.Status != 404 {
		t.Errorf("Status = %d, want 404", rerr.Status)
	}
}
