package rerout_test

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"strconv"
	"testing"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

const (
	webhookSecret  = "whsec_super_secret_value"
	webhookRawBody = `{"id":"evt_abc","type":"link.clicked","data":{"code":"q4"}}`
)

// signHeader computes a valid "t=…,v1=…" header for ts/body/secret.
func signHeader(ts int64, body, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(strconv.FormatInt(ts, 10) + "." + body))
	return "t=" + strconv.FormatInt(ts, 10) + ",v1=" + hex.EncodeToString(mac.Sum(nil))
}

// fixedClock returns a clock that always reports unixSeconds.
func fixedClock(unixSeconds int64) func() int64 {
	return func() int64 { return unixSeconds }
}

func TestVerifySignature_ValidFreshlySigned(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	ok := rerout.VerifySignature(
		webhookRawBody,
		signHeader(ts, webhookRawBody, webhookSecret),
		webhookSecret,
		rerout.WithClock(fixedClock(ts)),
	)
	if !ok {
		t.Error("VerifySignature = false, want true for a freshly signed payload")
	}
}

func TestVerifySignature_WrongHMACRejected(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	// Header signed with a different secret — the HMAC will not match.
	ok := rerout.VerifySignature(
		webhookRawBody,
		signHeader(ts, webhookRawBody, "whsec_other_secret"),
		webhookSecret,
		rerout.WithClock(fixedClock(ts)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for a wrong-secret HMAC")
	}
}

func TestVerifySignature_TamperedBodyRejected(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	header := signHeader(ts, webhookRawBody, webhookSecret)
	ok := rerout.VerifySignature(
		webhookRawBody+" ", // extra trailing space — body no longer matches
		header,
		webhookSecret,
		rerout.WithClock(fixedClock(ts)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for a tampered body")
	}
}

func TestVerifySignature_ExpiredOutsideToleranceRejected(t *testing.T) {
	t.Parallel()
	const signed int64 = 1700000000
	header := signHeader(signed, webhookRawBody, webhookSecret)
	ok := rerout.VerifySignature(
		webhookRawBody,
		header,
		webhookSecret,
		// One second past the tolerance window.
		rerout.WithClock(fixedClock(signed+rerout.DefaultSignatureToleranceSeconds+1)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for an expired payload")
	}
}

func TestVerifySignature_FutureOutsideToleranceRejected(t *testing.T) {
	t.Parallel()
	const signed int64 = 1700000000
	header := signHeader(signed, webhookRawBody, webhookSecret)
	ok := rerout.VerifySignature(
		webhookRawBody,
		header,
		webhookSecret,
		// Clock is *behind* the signature by more than the tolerance.
		rerout.WithClock(fixedClock(signed-rerout.DefaultSignatureToleranceSeconds-1)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for a far-future timestamp")
	}
}

func TestVerifySignature_AtToleranceBoundaryAccepted(t *testing.T) {
	t.Parallel()
	const signed int64 = 1700000000
	header := signHeader(signed, webhookRawBody, webhookSecret)
	cases := []struct {
		name string
		now  int64
	}{
		{"exactly at +tolerance", signed + rerout.DefaultSignatureToleranceSeconds},
		{"exactly at -tolerance", signed - rerout.DefaultSignatureToleranceSeconds},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			ok := rerout.VerifySignature(
				webhookRawBody,
				header,
				webhookSecret,
				rerout.WithClock(fixedClock(tc.now)),
			)
			if !ok {
				t.Errorf("VerifySignature = false, want true at the tolerance boundary")
			}
		})
	}
}

func TestVerifySignature_ZeroToleranceDisablesTimestampCheck(t *testing.T) {
	t.Parallel()
	const signed int64 = 1700000000
	header := signHeader(signed, webhookRawBody, webhookSecret)
	ok := rerout.VerifySignature(
		webhookRawBody,
		header,
		webhookSecret,
		rerout.WithTolerance(0),
		// Wildly out of range — but the timestamp check is disabled.
		rerout.WithClock(fixedClock(signed+10_000_000)),
	)
	if !ok {
		t.Error("VerifySignature = false, want true when tolerance=0 disables the timestamp check")
	}
}

func TestVerifySignature_CustomTolerance(t *testing.T) {
	t.Parallel()
	const signed int64 = 1700000000
	header := signHeader(signed, webhookRawBody, webhookSecret)
	cases := []struct {
		name      string
		tolerance int
		now       int64
		want      bool
	}{
		{"within custom 10s window", 10, signed + 9, true},
		{"at custom 10s boundary", 10, signed + 10, true},
		{"past custom 10s window", 10, signed + 11, false},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			ok := rerout.VerifySignature(
				webhookRawBody,
				header,
				webhookSecret,
				rerout.WithTolerance(tc.tolerance),
				rerout.WithClock(fixedClock(tc.now)),
			)
			if ok != tc.want {
				t.Errorf("VerifySignature = %v, want %v", ok, tc.want)
			}
		})
	}
}

func TestVerifySignature_MalformedHeadersRejected(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	tsStr := strconv.FormatInt(ts, 10)
	cases := []struct {
		name   string
		header string
	}{
		{"empty", ""},
		{"garbage", "garbage"},
		{"empty t value", "t=,v1=abcd"},
		{"missing t", "v1=abcd"},
		{"missing v1", "t=" + tsStr},
		{"non-numeric t", "t=notanumber,v1=abcd"},
		{"negative t", "t=-1,v1=abcd"},
		{"zero t", "t=0,v1=abcd"},
		{"non-hex v1", "t=" + tsStr + ",v1=nothex!!"},
		{"odd-length v1", "t=" + tsStr + ",v1=12345"},
		{"empty v1 value", "t=" + tsStr + ",v1="},
		{"only commas", ",,,"},
		{"no equals", "tv1"},
		{"v1 with spaces inside hex", "t=" + tsStr + ",v1=ab cd"},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			ok := rerout.VerifySignature(
				webhookRawBody,
				tc.header,
				webhookSecret,
				rerout.WithClock(fixedClock(ts)),
			)
			if ok {
				t.Errorf("VerifySignature = true, want false for malformed header %q", tc.header)
			}
		})
	}
}

func TestVerifySignature_HeaderKeyCasingVariations(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	mac := hmac.New(sha256.New, []byte(webhookSecret))
	mac.Write([]byte(strconv.FormatInt(ts, 10) + "." + webhookRawBody))
	hexMAC := hex.EncodeToString(mac.Sum(nil))
	cases := []struct {
		name   string
		header string
	}{
		{"upper T and V1", "T=" + strconv.FormatInt(ts, 10) + ",V1=" + hexMAC},
		{"mixed case keys", "T=" + strconv.FormatInt(ts, 10) + ",v1=" + hexMAC},
		{"spaces around segments", "  t = " + strconv.FormatInt(ts, 10) + " , v1 = " + hexMAC + " "},
		{"uppercase hex digest", "t=" + strconv.FormatInt(ts, 10) + ",v1=" + hexUpper(hexMAC)},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			t.Parallel()
			ok := rerout.VerifySignature(
				webhookRawBody,
				tc.header,
				webhookSecret,
				rerout.WithClock(fixedClock(ts)),
			)
			if !ok {
				t.Errorf("VerifySignature = false, want true for casing variant %q", tc.header)
			}
		})
	}
}

func TestVerifySignature_EmptySecretRejected(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	header := signHeader(ts, webhookRawBody, webhookSecret)
	ok := rerout.VerifySignature(
		webhookRawBody,
		header,
		"", // empty secret
		rerout.WithClock(fixedClock(ts)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for an empty secret")
	}
}

func TestVerifySignature_EmptyHeaderRejected(t *testing.T) {
	t.Parallel()
	ok := rerout.VerifySignature(
		webhookRawBody,
		"", // empty header
		webhookSecret,
		rerout.WithClock(fixedClock(1700000000)),
	)
	if ok {
		t.Error("VerifySignature = true, want false for an empty header")
	}
}

func TestVerifySignature_ExtraSegmentsIgnored(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	// Forward-compat: an unknown "v2=" segment must not break v1 verification.
	header := signHeader(ts, webhookRawBody, webhookSecret) + ",v2=somethingelse"
	ok := rerout.VerifySignature(
		webhookRawBody,
		header,
		webhookSecret,
		rerout.WithClock(fixedClock(ts)),
	)
	if !ok {
		t.Error("VerifySignature = false, want true when an unknown segment is present")
	}
}

func TestVerifySignature_EmptyBodySignsAndVerifies(t *testing.T) {
	t.Parallel()
	const ts int64 = 1700000000
	header := signHeader(ts, "", webhookSecret)
	ok := rerout.VerifySignature(
		"", // empty raw body is a legitimate payload
		header,
		webhookSecret,
		rerout.WithClock(fixedClock(ts)),
	)
	if !ok {
		t.Error("VerifySignature = false, want true for a correctly signed empty body")
	}
}

func TestVerifySignature_DefaultClockUsesNow(t *testing.T) {
	t.Parallel()
	// Without WithClock the verifier uses time.Now(). A header signed with a
	// far-past timestamp must therefore be rejected by the default tolerance.
	header := signHeader(1, webhookRawBody, webhookSecret)
	if rerout.VerifySignature(webhookRawBody, header, webhookSecret) {
		t.Error("VerifySignature = true, want false for an ancient timestamp under the default clock")
	}
}

// hexUpper uppercases an all-lowercase hex string for casing tests.
func hexUpper(s string) string {
	b := []byte(s)
	for i := range b {
		if b[i] >= 'a' && b[i] <= 'f' {
			b[i] -= 'a' - 'A'
		}
	}
	return string(b)
}
