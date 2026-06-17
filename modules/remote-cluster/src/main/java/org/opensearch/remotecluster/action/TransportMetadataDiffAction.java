/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.action;

import org.opensearch.action.admin.cluster.state.ClusterStateAction;
import org.opensearch.action.admin.cluster.state.ClusterStateRequest;
import org.opensearch.action.admin.cluster.state.ClusterStateResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.remotecluster.provider.IndexMetadataDiffProvider;
import org.opensearch.remotecluster.provider.IngestPipelineDiffProvider;
import org.opensearch.remotecluster.provider.LegacyTemplateDiffProvider;
import org.opensearch.remotecluster.spi.CategoryDiffResult;
import org.opensearch.remotecluster.spi.MetadataDiffProvider;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Transport action that fetches remote cluster metadata and computes diff.
 */
public class TransportMetadataDiffAction extends HandledTransportAction<MetadataDiffRequest, MetadataDiffResponse> {

    private final ClusterService clusterService;
    private final Client client;
    private final Map<String, MetadataDiffProvider> providers;

    @Inject
    public TransportMetadataDiffAction(
        TransportService transportService,
        ActionFilters actionFilters,
        ClusterService clusterService,
        Client client
    ) {
        super(MetadataDiffAction.NAME, transportService, actionFilters, MetadataDiffRequest::new);
        this.clusterService = clusterService;
        this.client = client;
        this.providers = new LinkedHashMap<>();
        providers.put("templates", new LegacyTemplateDiffProvider());
        providers.put("ingest_pipelines", new IngestPipelineDiffProvider());
        providers.put("indices", new IndexMetadataDiffProvider());
    }

    public Set<String> getSupportedCategories() {
        return providers.keySet();
    }

    @Override
    protected void doExecute(Task task, MetadataDiffRequest request, ActionListener<MetadataDiffResponse> listener) {
        ClusterStateRequest remoteRequest = new ClusterStateRequest().clear().metadata(true).customs(true);
        Client remoteClient = client.getRemoteClusterClient(request.getConnectionName());

        remoteClient.execute(ClusterStateAction.INSTANCE, remoteRequest, ActionListener.wrap((ClusterStateResponse remoteStateResponse) -> {
            try {
                Metadata localMetadata = clusterService.state().metadata();
                Metadata remoteMetadata = remoteStateResponse.getState().metadata();
                List<CategoryDiffResult> results = new ArrayList<>();
                for (Map.Entry<String, MetadataDiffProvider> entry : providers.entrySet()) {
                    if (request.getCategories().contains(entry.getKey())) {
                        results.add(entry.getValue().diff(localMetadata, remoteMetadata));
                    }
                }
                listener.onResponse(
                    new MetadataDiffResponse(request.getConnectionName(), remoteMetadata.version(), localMetadata.version(), results)
                );
            } catch (Exception e) {
                listener.onFailure(e);
            }
        }, listener::onFailure));
    }
}
