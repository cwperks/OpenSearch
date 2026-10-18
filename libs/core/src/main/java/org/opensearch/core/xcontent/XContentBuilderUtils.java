/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.core.xcontent;

import java.io.IOException;

/**
 * Small structural helpers for writing nested {@link XContentBuilder} responses.
 */
public final class XContentBuilderUtils {

    private XContentBuilderUtils() {}

    @FunctionalInterface
    public interface BuilderConsumer {
        void accept(XContentBuilder builder) throws IOException;
    }

    @FunctionalInterface
    public interface BuilderItemConsumer<T> {
        void accept(XContentBuilder builder, T item) throws IOException;
    }

    public static XContentBuilder object(XContentBuilder builder, BuilderConsumer body) throws IOException {
        builder.startObject();
        body.accept(builder);
        return builder.endObject();
    }

    public static XContentBuilder object(XContentBuilder builder, String name, BuilderConsumer body) throws IOException {
        builder.startObject(name);
        body.accept(builder);
        return builder.endObject();
    }

    public static XContentBuilder array(XContentBuilder builder, String name, BuilderConsumer body) throws IOException {
        builder.startArray(name);
        body.accept(builder);
        return builder.endArray();
    }

    public static <T> XContentBuilder objectArray(
        XContentBuilder builder,
        String name,
        Iterable<T> items,
        BuilderItemConsumer<T> itemWriter
    ) throws IOException {
        return array(builder, name, arrayBuilder -> {
            for (T item : items) {
                object(arrayBuilder, itemBuilder -> itemWriter.accept(itemBuilder, item));
            }
        });
    }
}
