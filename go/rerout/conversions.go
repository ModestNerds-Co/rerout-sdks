// Package rerout — Conversions namespace.
//
// Reached via Client.Conversions(). Wraps the /v1/conversions surface: record
// a conversion event against a click. Conversions attribute downstream value
// (purchases, sign-ups) back to the click that drove them.

package rerout

import "context"

// Conversions is the conversion-tracking namespace. Construct via
// Client.Conversions().
type Conversions struct {
	client *Client
}

// Record records a conversion event against a click via POST /v1/conversions.
//
// ClickID and EventName are required on input. The call is idempotent on the
// (click_id, event_name) pair: the returned ConversionResult reports whether a
// new row was Recorded and whether the event was a Duplicate of one already
// stored.
func (c *Conversions) Record(ctx context.Context, input RecordConversionInput) (*ConversionResult, error) {
	var out ConversionResult
	err := c.client.do(ctx, requestOptions{
		method: "POST",
		path:   "/v1/conversions",
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}
