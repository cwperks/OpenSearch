/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.http.netty4;

import org.opensearch.common.settings.Settings;
import org.opensearch.http.HttpTransportSettings;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class Netty4HstsHandlerTests extends OpenSearchTestCase {
    public void testPolicyOnSuccessAndErrors() {
        for (HttpResponseStatus status : new HttpResponseStatus[] {
            HttpResponseStatus.OK,
            HttpResponseStatus.UNAUTHORIZED,
            HttpResponseStatus.FORBIDDEN,
            HttpResponseStatus.BAD_REQUEST,
            HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE }) {
            assertHeader(Settings.builder().put("http.hsts.enabled", true).build(), status, "max-age=31536000");
        }
    }

    public void testDisabledByDefault() {
        assertHeader(Settings.EMPTY, HttpResponseStatus.OK, null);
    }

    public void testSubdomainsAndPolicyRemoval() {
        assertHeader(
            Settings.builder()
                .put("http.hsts.enabled", true)
                .put("http.hsts.max_age", "1h")
                .put("http.hsts.include_subdomains", true)
                .build(),
            HttpResponseStatus.OK,
            "max-age=3600; includeSubDomains"
        );
        assertHeader(
            Settings.builder().put("http.hsts.enabled", true).put("http.hsts.max_age", "0s").build(),
            HttpResponseStatus.OK,
            "max-age=0"
        );
    }

    public void testNegativeMaxAgeIsRejected() {
        expectThrows(
            IllegalArgumentException.class,
            () -> HttpTransportSettings.SETTING_HTTP_HSTS_MAX_AGE.get(Settings.builder().put("http.hsts.max_age", "-1s").build())
        );
    }

    public void testReplacesExistingPolicy() {
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            Netty4HstsHandler.configure(channel.pipeline(), Settings.builder().put("http.hsts.enabled", true).build());
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().add("Strict-Transport-Security", "max-age=1");
            response.headers().add("Strict-Transport-Security", "max-age=2");
            channel.writeOutbound(response);
            FullHttpResponse actual = channel.readOutbound();
            try {
                assertEquals(List.of("max-age=31536000"), actual.headers().getAll("Strict-Transport-Security"));
            } finally {
                actual.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private void assertHeader(Settings settings, HttpResponseStatus status, String expected) {
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            Netty4HstsHandler.configure(channel.pipeline(), settings);
            channel.writeOutbound(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status));
            FullHttpResponse response = channel.readOutbound();
            try {
                assertEquals(expected, response.headers().get("Strict-Transport-Security"));
            } finally {
                response.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
