// Package rerout — webhook signature verification.
//
// Rerout signs every webhook delivery as
//
//	X-Rerout-Signature: t={unix_seconds},v1={hex_hmac_sha256}
//
// where the HMAC is computed over `"{timestamp}.{raw_body}"` with the endpoint
// signing secret as the key. VerifySignature parses the header, checks the
// timestamp tolerance, and compares the HMAC in constant time.

package rerout

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"strconv"
	"strings"
	"time"
)

// DefaultSignatureToleranceSeconds is the default window (in seconds) between
// the `t=` timestamp on the signature and the current time. Five minutes —
// matches the TS / Dart reference SDKs.
const DefaultSignatureToleranceSeconds = 300

// SignatureOption customises VerifySignature.
type SignatureOption func(*signatureConfig)

type signatureConfig struct {
	tolerance int
	now       func() int64
}

// WithTolerance overrides the timestamp tolerance window in seconds. Pass 0
// to disable the timestamp check entirely.
func WithTolerance(seconds int) SignatureOption {
	return func(cfg *signatureConfig) { cfg.tolerance = seconds }
}

// WithClock injects a custom clock for deterministic tests. The function
// returns the current unix time in seconds.
func WithClock(now func() int64) SignatureOption {
	return func(cfg *signatureConfig) { cfg.now = now }
}

// VerifySignature verifies an X-Rerout-Signature header against rawBody using
// the endpoint signing secret. Returns true only when the timestamp is within
// the tolerance and the HMAC matches in constant time.
//
// Returns false when:
//   - signatureHeader or secret is empty,
//   - the header is malformed (missing `t=` or `v1=`, non-numeric or non-
//     positive `t`, non-hex or odd-length `v1`),
//   - the timestamp is outside the tolerance window (skipped when tolerance
//     is 0),
//   - the computed HMAC doesn't match the supplied v1.
//
// Key matching is case-insensitive (`T=` / `V1=` accepted), the hex digest
// is decoded case-insensitively, and the byte-level comparison is constant
// time (hmac.Equal).
//
//	ok := rerout.VerifySignature(
//	    rawBody,
//	    r.Header.Get("X-Rerout-Signature"),
//	    os.Getenv("REROUT_WEBHOOK_SECRET"),
//	)
//	if !ok {
//	    w.WriteHeader(http.StatusBadRequest)
//	    return
//	}
func VerifySignature(rawBody, signatureHeader, secret string, opts ...SignatureOption) bool {
	if signatureHeader == "" || secret == "" {
		return false
	}

	cfg := signatureConfig{
		tolerance: DefaultSignatureToleranceSeconds,
		now:       func() int64 { return time.Now().Unix() },
	}
	for _, opt := range opts {
		opt(&cfg)
	}

	ts, v1, ok := parseSignatureHeader(signatureHeader)
	if !ok {
		return false
	}

	if cfg.tolerance > 0 {
		now := cfg.now()
		delta := now - ts
		if delta < 0 {
			delta = -delta
		}
		if delta > int64(cfg.tolerance) {
			return false
		}
	}

	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(strconv.FormatInt(ts, 10)))
	mac.Write([]byte{'.'})
	mac.Write([]byte(rawBody))
	expected := mac.Sum(nil)

	actual, err := hex.DecodeString(v1)
	if err != nil {
		return false
	}
	return hmac.Equal(expected, actual)
}

// parseSignatureHeader pulls `t` and `v1` out of a "t=…,v1=…" header.
// Keys are matched case-insensitively. Returns ok=false on any malformation.
func parseSignatureHeader(header string) (timestamp int64, v1 string, ok bool) {
	var (
		gotTS = false
		gotV1 = false
	)
	for _, segment := range strings.Split(header, ",") {
		eq := strings.IndexByte(segment, '=')
		if eq <= 0 {
			continue
		}
		key := strings.ToLower(strings.TrimSpace(segment[:eq]))
		value := strings.TrimSpace(segment[eq+1:])
		switch key {
		case "t":
			if value == "" {
				continue
			}
			parsed, err := strconv.ParseInt(value, 10, 64)
			if err != nil || parsed <= 0 {
				continue
			}
			timestamp = parsed
			gotTS = true
		case "v1":
			if value == "" {
				continue
			}
			// Reject odd-length or non-hex up front so callers see false fast.
			if len(value)%2 != 0 {
				continue
			}
			if !isHex(value) {
				continue
			}
			v1 = value
			gotV1 = true
		}
	}
	if !gotTS || !gotV1 {
		return 0, "", false
	}
	return timestamp, v1, true
}

func isHex(s string) bool {
	for i := 0; i < len(s); i++ {
		c := s[i]
		switch {
		case c >= '0' && c <= '9':
		case c >= 'a' && c <= 'f':
		case c >= 'A' && c <= 'F':
		default:
			return false
		}
	}
	return true
}
