/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A tag attached to a [Link].
 *
 * Tags are read-only for API-key clients: they surface on link responses but
 * cannot be written through `create`/`update`.
 */
@Serializable
public data class Tag(
    /** Tag identifier. */
    val id: String,
    /** Human-readable tag name. */
    val name: String,
    /** Display color — typically a hex string such as `#3b82f6`. */
    val color: String,
)

/**
 * A short link as returned by the Rerout API.
 *
 * Field names mirror the server-side `LinkResponse` shape so JSON is parsed
 * without transformation.
 */
@Serializable
public data class Link(
    /** The short link path code. */
    val code: String,
    /** Fully-qualified short URL — `https://{host}/{code}`. */
    @SerialName("short_url") val shortUrl: String,
    /** Verified custom domain hosting this link, when one is bound. */
    @SerialName("domain_hostname") val domainHostname: String? = null,
    /** Destination the redirect resolves to. */
    @SerialName("target_url") val targetUrl: String,
    /** Project that owns the link. */
    @SerialName("project_id") val projectId: String,
    /** Unix seconds — expiration. Null for permanent links. */
    @SerialName("expires_at") val expiresAt: Long? = null,
    /** Whether the link is currently active. */
    @SerialName("is_active") val isActive: Boolean,
    /** Override social preview title. */
    @SerialName("seo_title") val seoTitle: String? = null,
    /** Override social preview description. */
    @SerialName("seo_description") val seoDescription: String? = null,
    /** Override social preview image URL. */
    @SerialName("seo_image_url") val seoImageUrl: String? = null,
    /** Override preview canonical URL. */
    @SerialName("seo_canonical_url") val seoCanonicalUrl: String? = null,
    /** Whether the preview HTML should be indexed. */
    @SerialName("seo_noindex") val seoNoindex: Boolean = true,
    /** Unix seconds — last SEO field change. */
    @SerialName("seo_updated_at") val seoUpdatedAt: Long? = null,
    /** Unix seconds — link creation time. */
    @SerialName("created_at") val createdAt: Long,
    /** Unix seconds — last mutation. */
    @SerialName("updated_at") val updatedAt: Long,
    /**
     * Tags attached to this link. Read-only — populated on reads and after a
     * `create`/`update`, empty when none are set. Defaults to empty so older
     * payloads without a `tags` field still parse.
     */
    val tags: List<Tag> = emptyList(),
)

/**
 * Request body for creating a short link.
 *
 * Only [targetUrl] is required. Optional fields are omitted from the JSON when
 * left `null` so server-side defaults apply.
 */
@Serializable
public data class CreateLinkInput(
    /** Absolute `https://` destination URL. */
    @SerialName("target_url") val targetUrl: String,
    /** Verified custom domain to host this link on. Omit for `rerout.co/:code`. */
    @SerialName("domain_hostname") val domainHostname: String? = null,
    /** Custom path. Only valid when [domainHostname] is provided. */
    val code: String? = null,
    /** Unix seconds — expiration. Omit for a permanent link. */
    @SerialName("expires_at") val expiresAt: Long? = null,
    /** Override social preview title. Max 90 characters. */
    @SerialName("seo_title") val seoTitle: String? = null,
    /** Override social preview description. Max 220 characters. */
    @SerialName("seo_description") val seoDescription: String? = null,
    /** Absolute `https://` social preview image URL. */
    @SerialName("seo_image_url") val seoImageUrl: String? = null,
    /** Canonical URL for the preview HTML. */
    @SerialName("seo_canonical_url") val seoCanonicalUrl: String? = null,
    /** Whether the preview page should be marked noindex. */
    @SerialName("seo_noindex") val seoNoindex: Boolean? = null,
)

/**
 * Request body for the `PATCH /v1/links/:code` endpoint.
 *
 * Every field is optional. The shape distinguishes *"leave the field alone"*
 * from *"set the field to null on the server"* via the `clear*` flags: an
 * unset value sends nothing, while a `clear*` flag sends an explicit `null`.
 *
 * Use [builder] for fluent construction, or the constructor directly.
 */
public class UpdateLinkInput private constructor(
    /** New destination URL, or `null` to leave unchanged. */
    public val targetUrl: String? = null,
    /** New expiry (unix seconds). Use [clearExpiresAt] to remove an existing one. */
    public val expiresAt: Long? = null,
    /** When true, sends `expires_at: null` to remove an existing expiry. */
    public val clearExpiresAt: Boolean = false,
    /** Activate or deactivate the link. */
    public val isActive: Boolean? = null,
    /** New preview title. */
    public val seoTitle: String? = null,
    /** When true, sends `seo_title: null` to clear an existing title. */
    public val clearSeoTitle: Boolean = false,
    /** New preview description. */
    public val seoDescription: String? = null,
    /** When true, sends `seo_description: null` to clear an existing description. */
    public val clearSeoDescription: Boolean = false,
    /** New preview image URL. */
    public val seoImageUrl: String? = null,
    /** When true, sends `seo_image_url: null` to clear an existing image URL. */
    public val clearSeoImageUrl: Boolean = false,
    /** New canonical URL. */
    public val seoCanonicalUrl: String? = null,
    /** When true, sends `seo_canonical_url: null` to clear an existing canonical URL. */
    public val clearSeoCanonicalUrl: Boolean = false,
    /** Toggle whether the preview page is noindex. */
    public val seoNoindex: Boolean? = null,
) {
    /**
     * Renders this patch into the JSON map the API expects. Fields are only
     * included when set — explicit nulls happen via the `clear*` flags so an
     * unset field never wipes server state.
     */
    public fun toJsonMap(): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        if (targetUrl != null) map["target_url"] = targetUrl
        if (clearExpiresAt) map["expires_at"] = null else if (expiresAt != null) map["expires_at"] = expiresAt
        if (isActive != null) map["is_active"] = isActive
        if (clearSeoTitle) map["seo_title"] = null else if (seoTitle != null) map["seo_title"] = seoTitle
        if (clearSeoDescription) {
            map["seo_description"] = null
        } else if (seoDescription != null) {
            map["seo_description"] = seoDescription
        }
        if (clearSeoImageUrl) map["seo_image_url"] = null else if (seoImageUrl != null) map["seo_image_url"] = seoImageUrl
        if (clearSeoCanonicalUrl) {
            map["seo_canonical_url"] = null
        } else if (seoCanonicalUrl != null) {
            map["seo_canonical_url"] = seoCanonicalUrl
        }
        if (seoNoindex != null) map["seo_noindex"] = seoNoindex
        return map
    }

    /** True when no field is set — the API would reject this as a no-op. */
    public val isEmpty: Boolean get() = toJsonMap().isEmpty()

    /** Fluent builder for [UpdateLinkInput]. */
    public class Builder {
        private var targetUrl: String? = null
        private var expiresAt: Long? = null
        private var clearExpiresAt: Boolean = false
        private var isActive: Boolean? = null
        private var seoTitle: String? = null
        private var clearSeoTitle: Boolean = false
        private var seoDescription: String? = null
        private var clearSeoDescription: Boolean = false
        private var seoImageUrl: String? = null
        private var clearSeoImageUrl: Boolean = false
        private var seoCanonicalUrl: String? = null
        private var clearSeoCanonicalUrl: Boolean = false
        private var seoNoindex: Boolean? = null

        /** Set a new destination URL. */
        public fun targetUrl(value: String): Builder = apply { targetUrl = value }

        /** Set a new expiry (unix seconds). */
        public fun expiresAt(value: Long): Builder = apply { expiresAt = value }

        /** Send `expires_at: null` to remove an existing expiry. */
        public fun clearExpiresAt(): Builder = apply { clearExpiresAt = true }

        /** Activate or deactivate the link. */
        public fun isActive(value: Boolean): Builder = apply { isActive = value }

        /** Set a new preview title. */
        public fun seoTitle(value: String): Builder = apply { seoTitle = value }

        /** Send `seo_title: null` to clear an existing title. */
        public fun clearSeoTitle(): Builder = apply { clearSeoTitle = true }

        /** Set a new preview description. */
        public fun seoDescription(value: String): Builder = apply { seoDescription = value }

        /** Send `seo_description: null` to clear an existing description. */
        public fun clearSeoDescription(): Builder = apply { clearSeoDescription = true }

        /** Set a new preview image URL. */
        public fun seoImageUrl(value: String): Builder = apply { seoImageUrl = value }

        /** Send `seo_image_url: null` to clear an existing image URL. */
        public fun clearSeoImageUrl(): Builder = apply { clearSeoImageUrl = true }

        /** Set a new canonical URL. */
        public fun seoCanonicalUrl(value: String): Builder = apply { seoCanonicalUrl = value }

        /** Send `seo_canonical_url: null` to clear an existing canonical URL. */
        public fun clearSeoCanonicalUrl(): Builder = apply { clearSeoCanonicalUrl = true }

        /** Toggle whether the preview page is noindex. */
        public fun seoNoindex(value: Boolean): Builder = apply { seoNoindex = value }

        /** Build the immutable [UpdateLinkInput]. */
        public fun build(): UpdateLinkInput = UpdateLinkInput(
            targetUrl = targetUrl,
            expiresAt = expiresAt,
            clearExpiresAt = clearExpiresAt,
            isActive = isActive,
            seoTitle = seoTitle,
            clearSeoTitle = clearSeoTitle,
            seoDescription = seoDescription,
            clearSeoDescription = clearSeoDescription,
            seoImageUrl = seoImageUrl,
            clearSeoImageUrl = clearSeoImageUrl,
            seoCanonicalUrl = seoCanonicalUrl,
            clearSeoCanonicalUrl = clearSeoCanonicalUrl,
            seoNoindex = seoNoindex,
        )
    }

    public companion object {
        /** Start a fluent [Builder] for an [UpdateLinkInput]. */
        public fun builder(): Builder = Builder()
    }
}

/** A paginated page of short links. */
@Serializable
public data class ListLinksResult(
    /** Links on this page, newest first. */
    val links: List<Link> = emptyList(),
    /** Cursor for the next page, or `null` when this is the last page. */
    @SerialName("next_cursor") val nextCursor: Long? = null,
) {
    /** Whether more pages remain. */
    val hasMore: Boolean get() = nextCursor != null
}

/** Result of a `delete` call. */
@Serializable
public data class DeleteResult(
    /** Whether the link was deleted. */
    val deleted: Boolean = true,
)

/** Info about the project that owns the current API key. */
@Serializable
public data class ProjectInfo(
    /** Project identifier. */
    val id: String,
    /** Human-readable project name. */
    val name: String,
    /** URL-safe project slug. */
    val slug: String,
)

/** A single bucket in an analytics breakdown — e.g. one country, one device. */
@Serializable
public data class StatsBreakdown(
    /** The bucket label — country code, device class, browser name, etc. */
    val value: String = "",
    /** Click count for this bucket. */
    val clicks: Long = 0,
)

/** A single point in a daily clicks time series. */
@Serializable
public data class DailyClicksPoint(
    /** Day bucket — unix seconds at 00:00 UTC. */
    val day: Long,
    /** Total clicks (link + QR) recorded that day. */
    val clicks: Long = 0,
    /** Subset of [clicks] that came from a QR scan. */
    @SerialName("qr_scans") val qrScans: Long = 0,
)

/** Aggregate analytics for a project across the requested window. */
@Serializable
public data class ProjectStats(
    /** Window size in days the totals span. */
    val days: Int = 0,
    /** Total clicks recorded in the window. */
    @SerialName("total_clicks") val totalClicks: Long = 0,
    /** Total QR scans (subset of [totalClicks]) recorded in the window. */
    @SerialName("qr_scans") val qrScans: Long = 0,
    /** One point per day across the window. Gap-filled by the server. */
    val daily: List<DailyClicksPoint> = emptyList(),
    /** Top countries by click count. */
    val countries: List<StatsBreakdown> = emptyList(),
    /** Top referrers by click count. */
    val referrers: List<StatsBreakdown> = emptyList(),
    /** Click share by device class (mobile / desktop / tablet / bot / unknown). */
    val devices: List<StatsBreakdown> = emptyList(),
    /** Click share by browser or in-app web view. */
    val browsers: List<StatsBreakdown> = emptyList(),
    /** Top short codes by click count. */
    @SerialName("top_codes") val topCodes: List<StatsBreakdown> = emptyList(),
)

/** Analytics for a single short link across the requested window. */
@Serializable
public data class LinkStats(
    /** The short code these stats belong to. */
    val code: String = "",
    /** Window size in days the totals span. */
    val days: Int = 0,
    /** Total clicks in the window. */
    @SerialName("total_clicks") val totalClicks: Long = 0,
    /** Subset of [totalClicks] attributed to a QR scan. */
    @SerialName("qr_scans") val qrScans: Long = 0,
    /** Top countries. */
    val countries: List<StatsBreakdown> = emptyList(),
    /** Top referrers. */
    val referrers: List<StatsBreakdown> = emptyList(),
)

/**
 * QR rendering parameters for [Qr.url] / [Qr.svg].
 *
 * All fields are optional — omit any to use the server default.
 */
public data class QrOptions(
    /** Module size in pixels. 1–32. Server default: 8. */
    val size: Int? = null,
    /** Quiet-zone modules. 0–16. Server default: 4. */
    val margin: Int? = null,
    /** Error-correction level — `L`, `M`, `Q`, or `H`. */
    val ecc: String? = null,
    /** Force the QR to encode a specific verified custom domain. */
    val domain: String? = null,
    /**
     * Cache-bust token. Passing [Refresh.enabled] sends `refresh=1`; any
     * non-empty token string is forwarded verbatim.
     */
    val refresh: Refresh? = null,
) {
    /** Cache-bust marker for [QrOptions.refresh]. */
    public sealed class Refresh {
        /** Sends `refresh=1`. */
        public data object Enabled : Refresh()

        /** Sends `refresh=<token>` verbatim. */
        public data class Token(val value: String) : Refresh()

        public companion object {
            /** Shorthand for [Refresh.Enabled]. */
            public val enabled: Refresh get() = Enabled

            /** Shorthand for a [Refresh.Token]. */
            public fun token(value: String): Refresh = Token(value)
        }
    }

    /** Render this options bag into ordered URL query pairs. */
    public fun toQueryParameters(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        if (size != null) out["size"] = size.toString()
        if (margin != null) out["margin"] = margin.toString()
        if (ecc != null) out["ecc"] = ecc
        if (domain != null) out["domain"] = domain
        when (val r = refresh) {
            null -> {}
            is Refresh.Enabled -> out["refresh"] = "1"
            is Refresh.Token -> out["refresh"] = r.value
        }
        return out
    }
}
