// Package rerout — Project namespace.
//
// Reached via Client.Project(). Wraps the /v1/projects/me/* surface:
// aggregate stats and current-project info.

package rerout

import (
	"context"
	"net/url"
)

// ProjectNS is the project-operations namespace. Construct via
// Client.Project(). The trailing "NS" disambiguates from the Project response
// type.
type ProjectNS struct {
	client *Client
}

// Stats returns aggregate analytics across every link in the project.
// Defaults to 30 days when days <= 0.
func (p *ProjectNS) Stats(ctx context.Context, days int) (*ProjectStats, error) {
	if days <= 0 {
		days = 30
	}
	q := url.Values{}
	q.Set("days", intToString(days))
	var out ProjectStats
	err := p.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/projects/me/stats",
		query:  q,
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}

// Me returns info about the project that owns the current API key.
func (p *ProjectNS) Me(ctx context.Context) (*Project, error) {
	var out Project
	err := p.client.do(ctx, requestOptions{
		method: "GET",
		path:   "/v1/projects/me",
	}, &out)
	if err != nil {
		return nil, err
	}
	return &out, nil
}
