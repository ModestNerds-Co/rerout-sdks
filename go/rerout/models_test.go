package rerout_test

import (
	"encoding/json"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── UpdateLinkInput.IsEmpty ─────────────────────────────────────────────

func TestUpdateLinkInput_IsEmpty(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name  string
		input rerout.UpdateLinkInput
		want  bool
	}{
		{"zero value", rerout.UpdateLinkInput{}, true},
		{"target_url set", rerout.UpdateLinkInput{TargetURL: rerout.String("https://x")}, false},
		{"expires_at set", rerout.UpdateLinkInput{ExpiresAt: rerout.Int64(123)}, false},
		{"clear expires_at", rerout.UpdateLinkInput{ClearExpiresAt: true}, false},
		{"is_active set", rerout.UpdateLinkInput{IsActive: rerout.Bool(false)}, false},
		{"seo_title set", rerout.UpdateLinkInput{SEOTitle: rerout.String("hi")}, false},
		{"clear seo_title", rerout.UpdateLinkInput{ClearSEOTitle: true}, false},
		{"clear seo_description", rerout.UpdateLinkInput{ClearSEODescription: true}, false},
		{"clear seo_image_url", rerout.UpdateLinkInput{ClearSEOImageURL: true}, false},
		{"clear seo_canonical_url", rerout.UpdateLinkInput{ClearSEOCanonicalURL: true}, false},
		{"seo_noindex set", rerout.UpdateLinkInput{SEONoindex: rerout.Bool(true)}, false},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			if got := tc.input.IsEmpty(); got != tc.want {
				t.Errorf("IsEmpty() = %v, want %v", got, tc.want)
			}
		})
	}
}

// ─── UpdateLinkInput.MarshalJSON ─────────────────────────────────────────

func TestUpdateLinkInput_MarshalJSON_OmitsUnset(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{TargetURL: rerout.String("https://example.com/new")}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	if len(got) != 1 {
		t.Errorf("expected exactly 1 key, got %d: %s", len(got), raw)
	}
	if got["target_url"] != "https://example.com/new" {
		t.Errorf("target_url = %v", got["target_url"])
	}
	for _, k := range []string{"expires_at", "is_active", "seo_title", "seo_image_url"} {
		if _, present := got[k]; present {
			t.Errorf("key %q should be omitted: %s", k, raw)
		}
	}
}

func TestUpdateLinkInput_MarshalJSON_ClearVsSetVsLeave(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{
		SEOTitle:         rerout.String("Black Friday"), // set
		ClearSEOImageURL: true,                          // explicit null
		// SEODescription left untouched
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	if got["seo_title"] != "Black Friday" {
		t.Errorf("seo_title = %v, want Black Friday", got["seo_title"])
	}
	v, present := got["seo_image_url"]
	if !present || v != nil {
		t.Errorf("seo_image_url = %v (present=%v), want explicit null", v, present)
	}
	if _, present := got["seo_description"]; present {
		t.Errorf("seo_description should be omitted (untouched): %s", raw)
	}
}

func TestUpdateLinkInput_MarshalJSON_ClearWinsOverSet(t *testing.T) {
	t.Parallel()
	// If both the value and the clear flag are set, the clear flag wins —
	// the field is sent as explicit null.
	in := rerout.UpdateLinkInput{
		ExpiresAt:      rerout.Int64(999),
		ClearExpiresAt: true,
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	v, present := got["expires_at"]
	if !present || v != nil {
		t.Errorf("expires_at = %v (present=%v), want explicit null", v, present)
	}
}

func TestUpdateLinkInput_MarshalJSON_AllFields(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{
		TargetURL:       rerout.String("https://example.com"),
		ExpiresAt:       rerout.Int64(1700000000),
		IsActive:        rerout.Bool(true),
		SEOTitle:        rerout.String("title"),
		SEODescription:  rerout.String("desc"),
		SEOImageURL:     rerout.String("https://img.example/x.png"),
		SEOCanonicalURL: rerout.String("https://example.com/canon"),
		SEONoindex:      rerout.Bool(false),
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	want := map[string]any{
		"target_url":        "https://example.com",
		"expires_at":        float64(1700000000),
		"is_active":         true,
		"seo_title":         "title",
		"seo_description":   "desc",
		"seo_image_url":     "https://img.example/x.png",
		"seo_canonical_url": "https://example.com/canon",
		"seo_noindex":       false,
	}
	if len(got) != len(want) {
		t.Errorf("got %d keys, want %d: %s", len(got), len(want), raw)
	}
	for k, wv := range want {
		if got[k] != wv {
			t.Errorf("%s = %v, want %v", k, got[k], wv)
		}
	}
}

// ─── CreateLinkInput JSON shape ──────────────────────────────────────────

func TestCreateLinkInput_OmitsUnsetOptionals(t *testing.T) {
	t.Parallel()
	in := rerout.CreateLinkInput{TargetURL: "https://example.com"}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	if len(got) != 1 || got["target_url"] != "https://example.com" {
		t.Errorf("expected only target_url, got: %s", raw)
	}
}

func TestCreateLinkInput_SEONoindexFalseIsSentWhenSet(t *testing.T) {
	t.Parallel()
	// seo_noindex is a *bool: explicitly setting it to false must still be
	// serialized (the pointer distinguishes "false" from "unset").
	in := rerout.CreateLinkInput{
		TargetURL:  "https://example.com",
		SEONoindex: rerout.Bool(false),
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var got map[string]any
	if err := json.Unmarshal(raw, &got); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	v, present := got["seo_noindex"]
	if !present || v != false {
		t.Errorf("seo_noindex = %v (present=%v), want false", v, present)
	}
}

// ─── Link decoding — nullable fields ─────────────────────────────────────

func TestLink_DecodesNullableFields(t *testing.T) {
	t.Parallel()
	const body = `{
	  "code":"q4","short_url":"https://rerout.co/q4","domain_hostname":null,
	  "target_url":"https://example.com","project_id":"p","expires_at":null,
	  "is_active":true,"seo_title":null,"seo_description":null,
	  "seo_image_url":null,"seo_canonical_url":null,"seo_noindex":false,
	  "seo_updated_at":null,"created_at":1,"updated_at":2
	}`
	var link rerout.Link
	if err := json.Unmarshal([]byte(body), &link); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	if link.DomainHostname != nil {
		t.Errorf("DomainHostname = %v, want nil", link.DomainHostname)
	}
	if link.ExpiresAt != nil {
		t.Errorf("ExpiresAt = %v, want nil", link.ExpiresAt)
	}
	if link.SEOTitle != nil {
		t.Errorf("SEOTitle = %v, want nil", link.SEOTitle)
	}
	if link.Code != "q4" || link.CreatedAt != 1 || link.UpdatedAt != 2 {
		t.Errorf("scalar fields wrong: %+v", link)
	}
}

// ─── pointer helpers ─────────────────────────────────────────────────────

func TestPointerHelpers(t *testing.T) {
	t.Parallel()
	if s := rerout.String("x"); s == nil || *s != "x" {
		t.Errorf("String() = %v", s)
	}
	if i := rerout.Int(7); i == nil || *i != 7 {
		t.Errorf("Int() = %v", i)
	}
	if i := rerout.Int64(99); i == nil || *i != 99 {
		t.Errorf("Int64() = %v", i)
	}
	if b := rerout.Bool(true); b == nil || *b != true {
		t.Errorf("Bool() = %v", b)
	}
	if b := rerout.Bool(false); b == nil || *b != false {
		t.Errorf("Bool(false) = %v", b)
	}
}

// ─── ListLinksResult.HasMore ─────────────────────────────────────────────

func TestListLinksResult_HasMore(t *testing.T) {
	t.Parallel()
	withCursor := rerout.ListLinksResult{NextCursor: rerout.Int64(10)}
	if !withCursor.HasMore() {
		t.Error("HasMore() = false, want true when NextCursor is set")
	}
	noCursor := rerout.ListLinksResult{}
	if noCursor.HasMore() {
		t.Error("HasMore() = true, want false when NextCursor is nil")
	}
}
