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
import org.opensearch.test.OpenSearchTestCase;

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

    public void testMarkerIsNotSerialized() throws Exception {
        ClusterHealthRequest request = new ClusterHealthRequest();
        request.markAsDerivedFromLocalAllIndices();

        try (BytesStreamOutput output = new BytesStreamOutput()) {
            request.writeTo(output);
            ClusterHealthRequest copy = new ClusterHealthRequest(output.bytes().streamInput());

            assertFalse(copy.isDerivedFromLocalAllIndices());
        }
    }
}
