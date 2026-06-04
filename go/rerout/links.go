// Package rerout — Links namespace.
//
// Reached via Client.Links(). Wraps the /v1/links/* surface: create, list,
// get, update, delete, stats.

package rerout

import (
	"context"
	"net/url"
)

// Links is the link-operations namespace. Construct via Client.Links().
type Links struct {
	client *Client
}

// Create creates a new short link.
func (l *Links) Create(ctx context.Context, input CreateLinkInput) (*Link, error) {
	var out Link
	err := l.client.do(ctx, requestOptions{
		method: "POST",
		path:   "/v1/links",
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// CreateBatch creates multiple links in a single request via POST
// /v1/links/batch. The returned BatchCreateLinksResult reports the per-item
// outcome: each result carries its input Index, an OK flag, and either the new
// Code (on success) or an Error string (on failure). A partial success is
// possible — Created may be less than Total.
//
// An empty input slice is a client-side error and never hits the API.
func (l *Links) CreateBatch(ctx context.Context, links []BatchLinkInput) (*BatchCreateLinksResult, error) {
	if len(links) == 0 {
		return nil, &ReroutError{
			Code:    CodeBadRequest,
			Status:  0,
			Message: "rerout: CreateBatch requires at least one link.",
			Path:    "/v1/links/batch",
		}
	}
	body := struct {
		Links []BatchLinkInput `json:"links"`
	}{Links: links}
	var out BatchCreateLinksResult
	err := l.client.do(ctx, requestOptions{
		method: "POST",
		path:   "/v1/links/batch",
		body:   body,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// List returns a paginated page of links. Pass nil for default cursor / limit.
func (l *Links) List(ctx context.Context, params *ListLinksParams) (*ListLinksResult, error) {
	var q url.Values
	if params != nil {
		q = url.Values{}
		if params.Cursor != nil {
			q.Set("cursor", int64String(*params.Cursor))
		}
		if params.Limit != nil {
			q.Set("limit", intToString(*params.Limit))
		}
	}
	var out ListLinksResult
	err := l.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/links",
		query:  q,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Get returns a single link by code.
func (l *Links) Get(ctx context.Context, code string) (*Link, error) {
	var out Link
	err := l.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/links/" + url.PathEscape(code),
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Update patches a link. Only fields set on input are sent. To explicitly
// clear an existing optional field server-side, set the matching
// `ClearXxx` flag on UpdateLinkInput.
//
// An empty UpdateLinkInput is a client-side error — it never hits the API.
func (l *Links) Update(ctx context.Context, code string, input UpdateLinkInput) (*Link, error) {
	if input.IsEmpty() {
		return nil, &ReroutError{
			Code:    CodeBadRequest,
			Status:  0,
			Message: "rerout: UpdateLinkInput has no fields to send.",
			Path:    "/v1/links/" + url.PathEscape(code),
		}
	}
	var out Link
	err := l.client.do(ctx, requestOptions{
		method: "PATCH",
		path:   "/v1/links/" + url.PathEscape(code),
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Delete soft-deletes a link. The short URL stops redirecting and disappears
// from list results.
func (l *Links) Delete(ctx context.Context, code string) (*DeleteResult, error) {
	var out DeleteResult
	err := l.client.do(ctx, requestOptions{
		method: "DELETE",
		path:   "/v1/links/" + url.PathEscape(code),
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Stats returns per-link click stats. The window defaults to 30 days when
// days <= 0.
func (l *Links) Stats(ctx context.Context, code string, days int) (*LinkStats, error) {
	if days <= 0 {
		days = 30
	}
	q := url.Values{}
	q.Set("days", intToString(days))
	var out LinkStats
	err := l.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/links/" + url.PathEscape(code) + "/stats",
		query:  q,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}
