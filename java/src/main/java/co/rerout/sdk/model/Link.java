/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A short link as returned by the Rerout API.
 *
 * <p>Field names mirror the server-side {@code LinkResponse} shape so JSON is
 * parsed without transformation. Instances are immutable.
 */
public final class Link {

    private final String code;

    @SerializedName("short_url")
    private final String shortUrl;

    @SerializedName("domain_hostname")
    private final String domainHostname;

    @SerializedName("target_url")
    private final String targetUrl;

    @SerializedName("project_id")
    private final String projectId;

    @SerializedName("expires_at")
    private final Long expiresAt;

    @SerializedName("is_active")
    private final boolean active;

    @SerializedName("seo_title")
    private final String seoTitle;

    @SerializedName("seo_description")
    private final String seoDescription;

    @SerializedName("seo_image_url")
    private final String seoImageUrl;

    @SerializedName("seo_canonical_url")
    private final String seoCanonicalUrl;

    @SerializedName("seo_noindex")
    private final boolean seoNoindex;

    @SerializedName("seo_updated_at")
    private final Long seoUpdatedAt;

    @SerializedName("created_at")
    private final long createdAt;

    @SerializedName("updated_at")
    private final long updatedAt;

    private final List<Tag> tags;

    /**
     * Creates a {@code Link}. Normally constructed by the SDK's JSON decoder;
     * exposed for tests and manual construction.
     *
     * @param code            short link path code
     * @param shortUrl        fully-qualified short URL
     * @param domainHostname  verified custom domain, or {@code null}
     * @param targetUrl       destination URL the redirect resolves to
     * @param projectId       project that owns the link
     * @param expiresAt       expiry in unix seconds, or {@code null} if permanent
     * @param active          whether the link is currently active
     * @param seoTitle        social preview title override, or {@code null}
     * @param seoDescription  social preview description override, or {@code null}
     * @param seoImageUrl     social preview image URL override, or {@code null}
     * @param seoCanonicalUrl preview canonical URL override, or {@code null}
     * @param seoNoindex      whether the preview HTML is marked noindex
     * @param seoUpdatedAt    last SEO field change in unix seconds, or {@code null}
     * @param createdAt       link creation time in unix seconds
     * @param updatedAt       last mutation time in unix seconds
     * @param tags            tags attached to the link; a {@code null} value is
     *                        normalised to an empty list
     */
    public Link(
            String code,
            String shortUrl,
            String domainHostname,
            String targetUrl,
            String projectId,
            Long expiresAt,
            boolean active,
            String seoTitle,
            String seoDescription,
            String seoImageUrl,
            String seoCanonicalUrl,
            boolean seoNoindex,
            Long seoUpdatedAt,
            long createdAt,
            long updatedAt,
            List<Tag> tags) {
        this.code = code;
        this.shortUrl = shortUrl;
        this.domainHostname = domainHostname;
        this.targetUrl = targetUrl;
        this.projectId = projectId;
        this.expiresAt = expiresAt;
        this.active = active;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.seoImageUrl = seoImageUrl;
        this.seoCanonicalUrl = seoCanonicalUrl;
        this.seoNoindex = seoNoindex;
        this.seoUpdatedAt = seoUpdatedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tags = tags == null ? Collections.emptyList() : tags;
    }

    /** {@return the short link path code} */
    public String getCode() {
        return code;
    }

    /** {@return the fully-qualified short URL — {@code https://{host}/{code}}} */
    public String getShortUrl() {
        return shortUrl;
    }

    /** {@return the verified custom domain hosting this link, or {@code null}} */
    public String getDomainHostname() {
        return domainHostname;
    }

    /** {@return the destination URL the redirect resolves to} */
    public String getTargetUrl() {
        return targetUrl;
    }

    /** {@return the identifier of the project that owns the link} */
    public String getProjectId() {
        return projectId;
    }

    /** {@return the expiry in unix seconds, or {@code null} for a permanent link} */
    public Long getExpiresAt() {
        return expiresAt;
    }

    /** {@return whether the link is currently active} */
    public boolean isActive() {
        return active;
    }

    /** {@return the social preview title override, or {@code null}} */
    public String getSeoTitle() {
        return seoTitle;
    }

    /** {@return the social preview description override, or {@code null}} */
    public String getSeoDescription() {
        return seoDescription;
    }

    /** {@return the social preview image URL override, or {@code null}} */
    public String getSeoImageUrl() {
        return seoImageUrl;
    }

    /** {@return the preview canonical URL override, or {@code null}} */
    public String getSeoCanonicalUrl() {
        return seoCanonicalUrl;
    }

    /** {@return whether the preview HTML should be marked noindex} */
    public boolean isSeoNoindex() {
        return seoNoindex;
    }

    /** {@return the last SEO field change in unix seconds, or {@code null}} */
    public Long getSeoUpdatedAt() {
        return seoUpdatedAt;
    }

    /** {@return the link creation time in unix seconds} */
    public long getCreatedAt() {
        return createdAt;
    }

    /** {@return the last mutation time in unix seconds} */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * {@return the tags attached to this link; never {@code null}}
     *
     * <p>Read-only: tag writes are not accepted from API-key clients. A missing
     * or {@code null} {@code tags} field in the response is normalised to an
     * empty list, and {@link co.rerout.sdk.Links#create create} returns an
     * empty list since newly created links carry no tags.
     */
    public List<Tag> getTags() {
        return tags == null ? Collections.emptyList() : tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Link)) {
            return false;
        }
        Link other = (Link) o;
        return active == other.active
                && seoNoindex == other.seoNoindex
                && createdAt == other.createdAt
                && updatedAt == other.updatedAt
                && Objects.equals(code, other.code)
                && Objects.equals(shortUrl, other.shortUrl)
                && Objects.equals(domainHostname, other.domainHostname)
                && Objects.equals(targetUrl, other.targetUrl)
                && Objects.equals(projectId, other.projectId)
                && Objects.equals(expiresAt, other.expiresAt)
                && Objects.equals(seoTitle, other.seoTitle)
                && Objects.equals(seoDescription, other.seoDescription)
                && Objects.equals(seoImageUrl, other.seoImageUrl)
                && Objects.equals(seoCanonicalUrl, other.seoCanonicalUrl)
                && Objects.equals(seoUpdatedAt, other.seoUpdatedAt)
                && Objects.equals(getTags(), other.getTags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                code, shortUrl, domainHostname, targetUrl, projectId, expiresAt,
                active, seoTitle, seoDescription, seoImageUrl, seoCanonicalUrl,
                seoNoindex, seoUpdatedAt, createdAt, updatedAt, getTags());
    }

    @Override
    public String toString() {
        return "Link{code=" + code + ", shortUrl=" + shortUrl
                + ", targetUrl=" + targetUrl + ", active=" + active + "}";
    }
}
