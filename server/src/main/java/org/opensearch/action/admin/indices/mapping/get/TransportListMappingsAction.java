/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache 2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.clustermanager.info.TransportClusterInfoAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.indices.IndicesService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transport action to list index mappings in pages.
 *
 * @opensearch.internal
 */
public class TransportListMappingsAction extends TransportClusterInfoAction<ListMappingsRequest, ListMappingsResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportListMappingsAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        IndicesService indicesService
    ) {
        super(
            ListMappingsAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            ListMappingsRequest::new,
            indexNameExpressionResolver
        );
        this.indicesService = indicesService;
    }

    @Override
    protected ListMappingsResponse read(StreamInput in) throws IOException {
        return new ListMappingsResponse(in);
    }

    @Override
    protected void doClusterManagerOperation(
        ListMappingsRequest request,
        String[] concreteIndices,
        ClusterState state,
        ActionListener<ListMappingsResponse> listener
    ) {
        try {
            final MappingPagination.Page page = MappingPagination.paginate(concreteIndices, request.pageParams());
            final Map<String, MappingMetadata> mappings = state.metadata()
                .findMappings(page.indices().toArray(String[]::new), indicesService.getFieldFilter());
            final Map<String, MappingMetadata> orderedMappings = new LinkedHashMap<>();
            for (String index : page.indices()) {
                final MappingMetadata mapping = mappings.get(index);
                if (mapping != null) {
                    orderedMappings.put(index, mapping);
                }
            }
            listener.onResponse(new ListMappingsResponse(orderedMappings, page.pageToken()));
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }
}
