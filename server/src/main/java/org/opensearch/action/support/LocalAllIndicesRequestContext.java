/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support;

import org.opensearch.common.annotation.InternalApi;
import org.opensearch.common.util.concurrent.ThreadContext;

/**
 * Local proof that Core marked a request as derived from an all-indices expression.
 *
 * @opensearch.internal
 */
@InternalApi
public final class LocalAllIndicesRequestContext {

    private static final String TRANSIENT_KEY = LocalAllIndicesRequestContext.class.getName();
    private static final Object MARKER = new Object();

    private LocalAllIndicesRequestContext() {}

    /**
     * Runs an action with local Core provenance when {@code request} is marked as derived from all indices.
     */
    public static void runWithContext(ThreadContext threadContext, LocalAllIndicesRequest request, Runnable action) {
        if (request.isDerivedFromLocalAllIndices() == false || isMarked(threadContext)) {
            action.run();
            return;
        }

        try (ThreadContext.StoredContext ignored = threadContext.newStoredContext(false)) {
            threadContext.putTransient(TRANSIENT_KEY, MARKER);
            action.run();
        }
    }

    /**
     * Returns whether Core marked the current local request.
     */
    public static boolean isMarked(ThreadContext threadContext) {
        return threadContext.getTransient(TRANSIENT_KEY) == MARKER;
    }

}
