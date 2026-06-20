// Package rerout — Tags management namespace.
//
// Reached via Client.Tags(). Wraps the /v1/projects/me/tags surface: list,
// create, update, delete. The project is resolved from the API key, so there
// is no project id in the path.

package rerout

import (
	"context"
	"net/url"
)

// Tags is the tag-management namespace. Construct via Client.Tags().
type Tags struct {
	client *Client
}

// List returns the project's tags with their live link counts. Each returned
// TagSummary carries the number of non-deleted links the tag is attached to.
func (t *Tags) List(ctx context.Context) (*ListTagsResult, error) {
	var out ListTagsResult
	err := t.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/projects/me/tags",
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Create creates a tag. Name is required; Color is optional and defaults
// server-side when omitted. The response omits the link count.
func (t *Tags) Create(ctx context.Context, input CreateTagInput) (*Tag, error) {
	var out Tag
	err := t.client.do(ctx, requestOptions{
		method: "POST",
		path:   "/v1/projects/me/tags",
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Update patches a tag's name and/or color. Only fields set on input are sent;
// omitted fields are left unchanged. Mirrors Links.Update: an empty
// UpdateTagInput is a client-side error and never hits the API.
func (t *Tags) Update(ctx context.Context, tagID string, input UpdateTagInput) (*Tag, error) {
	if input.IsEmpty() {
		return nil, &ReroutError{
			Code:    CodeBadRequest,
			Status:  0,
			Message: "rerout: UpdateTagInput has no fields to send.",
			Path:    "/v1/projects/me/tags/" + url.PathEscape(tagID),
		}
	}
	var out Tag
	err := t.client.do(ctx, requestOptions{
		method: "PATCH",
		path:   "/v1/projects/me/tags/" + url.PathEscape(tagID),
		body:   input,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Delete removes a tag and drops its assignments from all links. Tag ids look
// like "tag_…". The response body is {"deleted": true}.
func (t *Tags) Delete(ctx context.Context, tagID string) (*DeleteResult, error) {
	var out DeleteResult
	err := t.client.do(ctx, requestOptions{
		method: "DELETE",
		path:   "/v1/projects/me/tags/" + url.PathEscape(tagID),
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}
