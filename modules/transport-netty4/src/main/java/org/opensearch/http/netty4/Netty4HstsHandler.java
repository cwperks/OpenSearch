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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;

/** Adds the configured HSTS policy to HTTPS responses, including errors emitted before REST dispatch. */
public final class Netty4HstsHandler extends ChannelOutboundHandlerAdapter {
    private final String value;

    private Netty4HstsHandler(Settings settings) {
        value = "max-age="
            + HttpTransportSettings.SETTING_HTTP_HSTS_MAX_AGE.get(settings).seconds()
            + (HttpTransportSettings.SETTING_HTTP_HSTS_INCLUDE_SUBDOMAINS.get(settings) ? "; includeSubDomains" : "");
    }

    /** Installs HSTS on a secure pipeline after its HTTP codec, before handlers that can send responses. */
    public static void configure(ChannelPipeline pipeline, Settings settings) {
        if (HttpTransportSettings.SETTING_HTTP_HSTS_ENABLED.get(settings)) {
            pipeline.addLast("hsts", new Netty4HstsHandler(settings));
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof HttpResponse response) {
            response.headers().set("Strict-Transport-Security", value);
        }
        ctx.write(message, promise);
    }
}
