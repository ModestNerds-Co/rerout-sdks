// Package rerout — Webhooks management namespace.
//
// Reached via Client.Webhooks(). Wraps the /v1/projects/me/webhooks surface:
// create, list, delete. The project is resolved from the API key, so there is
// no project id in the path.
//
// This is distinct from the inbound signature verification helper
// (VerifySignature in webhooks.go), which validates deliveries the server
// sends to you.

package rerout

import (
	"context"
	"net/url"
)

// Webhooks is the webhook-endpoint-management namespace. Construct via
// Client.Webhooks().
type Webhooks struct {
	client *Client
}

// Create registers a webhook endpoint for the project that owns the API key.
//
// The returned CreatedWebhook carries a SigningSecret ("whsec_…") that is
// shown ONCE — persist it now so you can verify deliveries with
// VerifySignature; it is never returned again.
func (w *Webhooks) Create(ctx context.Context, input CreateWebhookInput) (*CreatedWebhook, error) {
	var out CreatedWebhook
	err := w.client.do(ctx, requestOptions{
		method: "POST",
		path:   "/v1/projects/me/webhooks",
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// List returns the project's webhook endpoints and the event types the server
// can deliver.
func (w *Webhooks) List(ctx context.Context) (*ListWebhooksResult, error) {
	var out ListWebhooksResult
	err := w.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/projects/me/webhooks",
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Delete soft-deletes a webhook endpoint and abandons its pending deliveries.
// Endpoint ids look like "wh_…". The operation is idempotent.
func (w *Webhooks) Delete(ctx context.Context, endpointID string) (*DeleteResult, error) {
	var out DeleteResult
	err := w.client.do(ctx, requestOptions{
		method: "DELETE",
		path:   "/v1/projects/me/webhooks/" + url.PathEscape(endpointID),
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}
