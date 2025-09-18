/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.common.util.concurrent;

public class ExecutionContext {
    private final ThreadLocal<String> context = new ThreadLocal<>();

    public void set(String value) {
        if (context.get() != null) {
            throw new IllegalArgumentException("ExecutionContext already present");
        }
        context.set(value);
    }

    public String get() {
        return context.get();
    }

    public void clear() {
        context.remove();
    }
}
