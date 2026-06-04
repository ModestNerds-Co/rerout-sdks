package rerout_test

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

// ─── Link response: Smart Links fields ───────────────────────────────────

func TestLink_DecodesSmartLinksFields(t *testing.T) {
	t.Parallel()
	body := `{
	  "code":"q4",
	  "short_url":"https://rerout.co/q4",
	  "target_url":"https://example.com",
	  "project_id":"prj_1",
	  "is_active":true,
	  "seo_noindex":false,
	  "tags":[],
	  "password_protected":true,
	  "max_clicks":1000,
	  "click_count":42,
	  "track_conversions":true,
	  "routing_rules":[
	    {"condition_type":"country","condition_op":"is","condition_value":"US","target_url":"https://example.com/us"},
	    {"condition_type":"device","condition_op":"in","condition_value":"ios,android","target_url":"https://example.com/app"}
	  ],
	  "ab_variants":[
	    {"id":1,"target_url":"https://example.com/a","weight":60},
	    {"id":2,"target_url":"https://example.com/b","weight":40}
	  ],
	  "created_at":1,
	  "updated_at":2
	}`
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(body))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Get(context.Background(), "q4")
	if err != nil {
		t.Fatalf("Get: %v", err)
	}
	if !link.PasswordProtected {
		t.Error("PasswordProtected = false, want true")
	}
	if link.MaxClicks == nil || *link.MaxClicks != 1000 {
		t.Errorf("MaxClicks = %v, want 1000", link.MaxClicks)
	}
	if link.ClickCount != 42 {
		t.Errorf("ClickCount = %d, want 42", link.ClickCount)
	}
	if !link.TrackConversions {
		t.Error("TrackConversions = false, want true")
	}
	if len(link.RoutingRules) != 2 {
		t.Fatalf("len(RoutingRules) = %d, want 2", len(link.RoutingRules))
	}
	if link.RoutingRules[0].ConditionType != "country" ||
		link.RoutingRules[0].ConditionOp != "is" ||
		link.RoutingRules[0].ConditionValue != "US" ||
		link.RoutingRules[0].TargetURL != "https://example.com/us" {
		t.Errorf("RoutingRules[0] = %+v", link.RoutingRules[0])
	}
	if link.RoutingRules[1].ConditionType != "device" || link.RoutingRules[1].ConditionOp != "in" {
		t.Errorf("RoutingRules[1] = %+v", link.RoutingRules[1])
	}
	if len(link.ABVariants) != 2 {
		t.Fatalf("len(ABVariants) = %d, want 2", len(link.ABVariants))
	}
	if link.ABVariants[0].ID != 1 || link.ABVariants[0].Weight != 60 ||
		link.ABVariants[0].TargetURL != "https://example.com/a" {
		t.Errorf("ABVariants[0] = %+v", link.ABVariants[0])
	}
}

func TestLink_DecodesNullMaxClicks(t *testing.T) {
	t.Parallel()
	body := `{"code":"a","short_url":"https://rerout.co/a","target_url":"https://x.com","project_id":"p","is_active":true,"seo_noindex":false,"tags":[],"password_protected":false,"max_clicks":null,"click_count":0,"track_conversions":false,"routing_rules":[],"ab_variants":[],"created_at":1,"updated_at":2}`
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(body))
	})
	defer srv.Close()

	c := newClient(t, srv)
	link, err := c.Links().Get(context.Background(), "a")
	if err != nil {
		t.Fatalf("Get: %v", err)
	}
	if link.MaxClicks != nil {
		t.Errorf("MaxClicks = %v, want nil", link.MaxClicks)
	}
	if link.PasswordProtected {
		t.Error("PasswordProtected = true, want false")
	}
}

// ─── CreateLinkInput: Smart Links fields ─────────────────────────────────

func TestCreateLinkInput_SendsSmartLinksFields(t *testing.T) {
	t.Parallel()
	var body string
	srv := newTestServer(func(w http.ResponseWriter, r *http.Request) {
		body = readBody(r)
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(linkJSON))
	})
	defer srv.Close()

	c := newClient(t, srv)
	_, err := c.Links().Create(context.Background(), rerout.CreateLinkInput{
		TargetURL:        "https://example.com",
		Password:         rerout.String("hunter2"),
		MaxClicks:        rerout.Int64(500),
		TrackConversions: rerout.Bool(true),
		RoutingRules: []rerout.RoutingRule{
			{ConditionType: "country", ConditionOp: "is", ConditionValue: "US", TargetURL: "https://example.com/us"},
		},
		ABVariants: []rerout.CreateABVariantInput{
			{TargetURL: "https://example.com/a", Weight: rerout.Int(70)},
			{TargetURL: "https://example.com/b"},
		},
	})
	if err != nil {
		t.Fatalf("Create: %v", err)
	}
	for _, want := range []string{
		`"password":"hunter2"`,
		`"max_clicks":500`,
		`"track_conversions":true`,
		`"condition_type":"country"`,
		`"condition_op":"is"`,
		`"target_url":"https://example.com/us"`,
		`"weight":70`,
	} {
		if !strings.Contains(body, want) {
			t.Errorf("body missing %s: %s", want, body)
		}
	}
	// Variant without an explicit weight omits the field.
	if strings.Count(body, `"weight"`) != 1 {
		t.Errorf("expected exactly one weight key, got body: %s", body)
	}
}

func TestCreateLinkInput_OmitsSmartLinksWhenUnset(t *testing.T) {
	t.Parallel()
	in := rerout.CreateLinkInput{TargetURL: "https://example.com"}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	got := string(raw)
	for _, forbidden := range []string{"password", "max_clicks", "track_conversions", "routing_rules", "ab_variants"} {
		if strings.Contains(got, forbidden) {
			t.Errorf("expected %s omitted, got %s", forbidden, got)
		}
	}
}

// ─── UpdateLinkInput: Smart Links marshal ────────────────────────────────

func TestUpdateLinkInput_MarshalSmartLinks_SetVsClear(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{
		Password:         rerout.String("newpass"),
		ClearMaxClicks:   true,
		TrackConversions: rerout.Bool(false),
		RoutingRules: &[]rerout.RoutingRule{
			{ConditionType: "device", ConditionOp: "is_not", ConditionValue: "ios", TargetURL: "https://example.com/web"},
		},
		ABVariants: &[]rerout.CreateABVariantInput{},
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	var m map[string]json.RawMessage
	if err := json.Unmarshal(raw, &m); err != nil {
		t.Fatalf("Unmarshal: %v", err)
	}
	if string(m["password"]) != `"newpass"` {
		t.Errorf("password = %s, want \"newpass\"", m["password"])
	}
	if string(m["max_clicks"]) != "null" {
		t.Errorf("max_clicks = %s, want null", m["max_clicks"])
	}
	if string(m["track_conversions"]) != "false" {
		t.Errorf("track_conversions = %s, want false", m["track_conversions"])
	}
	if _, ok := m["routing_rules"]; !ok {
		t.Error("routing_rules missing")
	}
	// A non-nil empty ABVariants slice serializes as an explicit empty array.
	if string(m["ab_variants"]) != "[]" {
		t.Errorf("ab_variants = %s, want []", m["ab_variants"])
	}
}

func TestUpdateLinkInput_ClearPasswordWinsOverSet(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{
		Password:      rerout.String("ignored"),
		ClearPassword: true,
	}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	if !strings.Contains(string(raw), `"password":null`) {
		t.Errorf("expected password:null, got %s", raw)
	}
}

func TestUpdateLinkInput_SmartLinksLeftAloneWhenUnset(t *testing.T) {
	t.Parallel()
	in := rerout.UpdateLinkInput{TargetURL: rerout.String("https://example.com/new")}
	raw, err := json.Marshal(in)
	if err != nil {
		t.Fatalf("Marshal: %v", err)
	}
	got := string(raw)
	for _, forbidden := range []string{"password", "max_clicks", "track_conversions", "routing_rules", "ab_variants"} {
		if strings.Contains(got, forbidden) {
			t.Errorf("expected %s omitted, got %s", forbidden, got)
		}
	}
}

func TestUpdateLinkInput_IsEmpty_SmartLinks(t *testing.T) {
	t.Parallel()
	cases := []struct {
		name  string
		input rerout.UpdateLinkInput
		want  bool
	}{
		{"password", rerout.UpdateLinkInput{Password: rerout.String("x")}, false},
		{"clear password", rerout.UpdateLinkInput{ClearPassword: true}, false},
		{"max_clicks", rerout.UpdateLinkInput{MaxClicks: rerout.Int64(10)}, false},
		{"clear max_clicks", rerout.UpdateLinkInput{ClearMaxClicks: true}, false},
		{"track_conversions", rerout.UpdateLinkInput{TrackConversions: rerout.Bool(true)}, false},
		{"routing_rules", rerout.UpdateLinkInput{RoutingRules: &[]rerout.RoutingRule{}}, false},
		{"ab_variants", rerout.UpdateLinkInput{ABVariants: &[]rerout.CreateABVariantInput{}}, false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if got := tc.input.IsEmpty(); got != tc.want {
				t.Errorf("IsEmpty() = %v, want %v", got, tc.want)
			}
		})
	}
}
