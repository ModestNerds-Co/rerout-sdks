// Package rerout — QR namespace.
//
// Reached via Client.QR(). Exposes a pure URL builder (URL) and an
// authenticated SVG fetcher (SVG).

package rerout

import (
	"context"
	"net/url"
	"sort"
	"strings"
)

// QR is the QR-helpers namespace. Construct via Client.QR().
type QR struct {
	client *Client
}

// URL builds the URL the Rerout API serves the QR SVG from. Pure — does not
// call the API. Authentication is the caller's responsibility, since plain
// <img src=…> tags can't add a bearer token; typically you pass the URL
// through a server-side proxy.
//
//	u := c.QR().URL("q4", nil)
//	// → https://api.rerout.co/v1/links/q4/qr
//
//	branded := c.QR().URL("q4", &rerout.QROptions{
//	    Size:   rerout.Int(12),
//	    ECC:    "H",
//	    Domain: "go.brand.com",
//	})
func (q *QR) URL(code string, options *QROptions) string {
	return BuildQRURL(q.client.BaseURL(), code, options)
}

// SVG fetches the rendered QR as an SVG string. Hits the same endpoint as
// URL but attaches the bearer token and returns the body.
func (q *QR) SVG(ctx context.Context, code string, options *QROptions) (string, error) {
	q2 := qrQueryValues(options)
	var out string
	err := q.client.do(ctx, requestOptions{
		method:      "GET",
		path:        "/v1/links/" + url.PathEscape(code) + "/qr",
		query:       q2,
		rawResponse: true,
	}, &out)
	if err != nil {
		return "", err
	}
	return out, nil
}

// BuildQRURL is the standalone version of QR.URL. Useful when you have a
// base URL handy but not a full Client (e.g. server-side template helpers).
//
// Trailing slashes on baseURL are stripped; the code is path-escaped.
func BuildQRURL(baseURL, code string, options *QROptions) string {
	base := trimTrailingSlashes(baseURL)
	path := base + "/v1/links/" + url.PathEscape(code) + "/qr"
	values := qrQueryValues(options)
	if len(values) == 0 {
		return path
	}
	// Use a deterministic ordering so the function is pure & testable.
	keys := make([]string, 0, len(values))
	for k := range values {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	var sb strings.Builder
	sb.WriteString(path)
	sb.WriteByte('?')
	for i, k := range keys {
		if i > 0 {
			sb.WriteByte('&')
		}
		sb.WriteString(url.QueryEscape(k))
		sb.WriteByte('=')
		sb.WriteString(url.QueryEscape(values.Get(k)))
	}
	return sb.String()
}

func qrQueryValues(options *QROptions) url.Values {
	q := options.toQueryParameters()
	if len(q) == 0 {
		return nil
	}
	out := make(url.Values, len(q))
	for k, v := range q {
		out.Set(k, v)
	}
	return out
}
