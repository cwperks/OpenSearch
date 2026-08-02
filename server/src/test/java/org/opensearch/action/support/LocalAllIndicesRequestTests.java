/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support;

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.indices.stats.IndicesStatsRequest;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.test.OpenSearchTestCase;

import java.util.concurrent.atomic.AtomicBoolean;

public class LocalAllIndicesRequestTests extends OpenSearchTestCase {

    public void testMarksAllIndicesExpressions() {
        for (String[] indices : new String[][] { null, new String[0], new String[] { "_all" }, new String[] { "*" } }) {
            ClusterHealthRequest request = new ClusterHealthRequest();

            LocalAllIndicesRequest.markIfAllIndices(request, indices);

            assertTrue(request.isDerivedFromLocalAllIndices());
        }
    }

    public void testDoesNotMarkExplicitIndicesExpressions() {
        for (String[] indices : new String[][] {
            new String[] { "index" },
            new String[] { "index-*" },
            new String[] { "index", "other" } }) {
            IndicesStatsRequest request = new IndicesStatsRequest();

            LocalAllIndicesRequest.markIfAllIndices(request, indices);

            assertFalse(request.isDerivedFromLocalAllIndices());
        }
    }

    public void testClusterHealthMarkerIsSerialized() throws Exception {
        ClusterHealthRequest request = new ClusterHealthRequest();
        request.markAsDerivedFromLocalAllIndices();

        try (BytesStreamOutput output = new BytesStreamOutput()) {
            request.writeTo(output);
            ClusterHealthRequest copy = new ClusterHealthRequest(output.bytes().streamInput());

            assertTrue(copy.isDerivedFromLocalAllIndices());
        }
    }

    public void testIndicesStatsMarkerIsSerialized() throws Exception {
        IndicesStatsRequest request = new IndicesStatsRequest();
        request.markAsDerivedFromLocalAllIndices();

        try (BytesStreamOutput output = new BytesStreamOutput()) {
            request.writeTo(output);
            IndicesStatsRequest copy = new IndicesStatsRequest(output.bytes().streamInput());

            assertTrue(copy.isDerivedFromLocalAllIndices());
        }
    }

    public void testLocalContextProofIsScopedToMarkedRequest() {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);
        ClusterHealthRequest request = new ClusterHealthRequest();
        AtomicBoolean actionCalled = new AtomicBoolean();

        LocalAllIndicesRequestContext.runWithContext(threadContext, request, () -> {
            actionCalled.set(true);
            assertFalse(LocalAllIndicesRequestContext.isMarked(threadContext));
        });
        assertTrue(actionCalled.get());

        request.markAsDerivedFromLocalAllIndices();
        LocalAllIndicesRequestContext.runWithContext(threadContext, request, () -> {
            assertTrue(LocalAllIndicesRequestContext.isMarked(threadContext));
        });
        assertFalse(LocalAllIndicesRequestContext.isMarked(threadContext));
    }
}
