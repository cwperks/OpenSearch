/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support;

import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.common.annotation.InternalApi;

import java.util.Arrays;

/**
 * Local-only provenance for a request that was derived from an all-indices expression.
 *
 * Coordinating actions can resolve an all-indices expression into concrete index names before
 * issuing child requests. This marker lets authorization plugins distinguish those derived
 * requests from requests that explicitly named the same concrete indices. Implementations must
 * not serialize the marker.
 *
 * @opensearch.internal
 */
@InternalApi
public interface LocalAllIndicesRequest {

    /**
     * Marks this request as derived from an all-indices expression.
     */
    void markAsDerivedFromLocalAllIndices();

    /**
     * Returns whether this request was derived from an all-indices expression.
     */
    boolean isDerivedFromLocalAllIndices();

    /**
     * Marks {@code request} when {@code originalIndices} targets all local indices.
     */
    static void markIfAllIndices(LocalAllIndicesRequest request, String[] originalIndices) {
        if (isAllIndices(originalIndices)) {
            request.markAsDerivedFromLocalAllIndices();
        }
    }

    private static boolean isAllIndices(String[] indices) {
        return IndexNameExpressionResolver.isAllIndices(indices == null ? null : Arrays.asList(indices))
            || (indices != null && indices.length == 1 && "*".equals(indices[0]));
    }
}
