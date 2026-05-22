/*
 * rerout-java — QR helper tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.rerout.sdk.MockApi.MockResponse;
import co.rerout.sdk.model.QrOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QrTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    // ─── url builder ────────────────────────────────────────────────────────

    @Test
    void bareUrlHasNoQueryString() {
        Rerout rerout = Rerout.create("rrk_test");
        assertEquals("https://api.rerout.co/v1/links/q4/qr", rerout.qr().url("q4"));
    }

    @Test
    void urlHonoursACustomBaseUrl() {
        Rerout rerout = Rerout.builder("rrk_test")
                .baseUrl("https://api.staging.rerout.co")
                .build();
        assertEquals(
                "https://api.staging.rerout.co/v1/links/q4/qr",
                rerout.qr().url("q4"));
    }

    @Test
    void urlEmitsEveryOption() {
        Rerout rerout = Rerout.create("rrk_test");
        String url = rerout.qr().url("q4", QrOptions.builder()
                .size(12)
                .margin(2)
                .ecc("H")
                .domain("go.brand.com")
                .build());
        assertTrue(url.startsWith("https://api.rerout.co/v1/links/q4/qr?"));
        assertTrue(url.contains("size=12"));
        assertTrue(url.contains("margin=2"));
        assertTrue(url.contains("ecc=H"));
        assertTrue(url.contains("domain=go.brand.com"));
    }

    @Test
    void urlWithRefreshEnabledEmits1() {
        Rerout rerout = Rerout.create("rrk_test");
        String url = rerout.qr().url("q4", QrOptions.builder().refreshEnabled().build());
        assertTrue(url.contains("refresh=1"));
    }

    @Test
    void urlWithARefreshTokenForwardsItVerbatim() {
        Rerout rerout = Rerout.create("rrk_test");
        String url = rerout.qr().url("q4", QrOptions.builder().refreshToken("v2").build());
        assertTrue(url.contains("refresh=v2"));
    }

    @Test
    void urlEncodesACodeWithSpecialCharacters() {
        Rerout rerout = Rerout.create("rrk_test");
        assertEquals(
                "https://api.rerout.co/v1/links/go%2Fpromo/qr",
                rerout.qr().url("go/promo"));
    }

    @Test
    void urlEncodesACodeWithASpace() {
        Rerout rerout = Rerout.create("rrk_test");
        assertEquals(
                "https://api.rerout.co/v1/links/hello%20world/qr",
                rerout.qr().url("hello world"));
    }

    @Test
    void urlWithOnlySomeOptionsOmitsTheRest() {
        Rerout rerout = Rerout.create("rrk_test");
        assertEquals(
                "https://api.rerout.co/v1/links/q4/qr?size=8",
                rerout.qr().url("q4", QrOptions.builder().size(8).build()));
    }

    // ─── svg fetch ──────────────────────────────────────────────────────────

    @Test
    void svgReturnsTheRenderedBody() {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
        api.enqueue(MockResponse.text(svg, 200, "image/svg+xml"));
        assertEquals(svg, api.client().qr().svg("q4"));
    }

    @Test
    void svgSendsTheBearerToken() {
        api.enqueue(MockResponse.text("<svg></svg>", 200, "image/svg+xml"));
        api.client("rrk_secret").qr().svg("q4");
        assertEquals("Bearer rrk_secret", api.takeRequest().header("Authorization"));
    }

    @Test
    void svgForwardsOptionsAsQueryParams() {
        api.enqueue(MockResponse.text("<svg></svg>", 200, "image/svg+xml"));
        api.client().qr().svg("q4", QrOptions.builder().size(16).ecc("Q").build());
        String path = api.takeRequest().path;
        assertTrue(path.startsWith("/v1/links/q4/qr?"));
        assertTrue(path.contains("size=16"));
        assertTrue(path.contains("ecc=Q"));
    }

    @Test
    void svgSurfacesANotFoundError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"not_found\",\"message\":\"no link\"}", 404));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().qr().svg("nope"));
        assertEquals("not_found", ex.getCode());
    }

    @Test
    void svgRaisesUnexpectedResponseOnAnEmptyBody() {
        api.enqueue(MockResponse.status(200));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().qr().svg("q4"));
        assertEquals("unexpected_response", ex.getCode());
    }

    @Test
    void svgAsyncCompletesWithTheRenderedBody() throws Exception {
        api.enqueue(MockResponse.text("<svg/>", 200, "image/svg+xml"));
        assertEquals("<svg/>", api.client().qr().svgAsync("q4").get());
    }
}
