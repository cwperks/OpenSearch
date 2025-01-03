/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.sample.action.create;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.Writeable;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.plugins.resource.SharableResource;
import org.opensearch.plugins.resource.sample.SampleResource;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.io.IOException;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Transport action for CreateSampleResource.
 */
public class CreateSampleResourceTransportAction extends HandledTransportAction<CreateSampleResourceRequest, CreateSampleResourceResponse> {
    private static final Logger log = LogManager.getLogger(CreateSampleResourceTransportAction.class);

    private final TransportService transportService;
    private final Client nodeClient;
    private final String resourceIndex;

    public CreateSampleResourceTransportAction(
        TransportService transportService,
        ActionFilters actionFilters,
        Client nodeClient,
        String actionName,
        String resourceIndex,
        Writeable.Reader<SampleResource> resourceReader
    ) {
        super(actionName, transportService, actionFilters, (in) -> new CreateSampleResourceRequest(in, resourceReader));
        this.transportService = transportService;
        this.nodeClient = nodeClient;
        this.resourceIndex = resourceIndex;
    }

    @Override
    protected void doExecute(Task task, CreateSampleResourceRequest request, ActionListener<CreateSampleResourceResponse> listener) {
        try (ThreadContext.StoredContext ignore = transportService.getThreadPool().getThreadContext().stashContext()) {
            CreateIndexRequest cir = new CreateIndexRequest(resourceIndex);
            ActionListener<CreateIndexResponse> cirListener = ActionListener.wrap(
                response -> { createResource(request, listener); },
                (failResponse) -> {
                    /* Index already exists, ignore and continue */
                    createResource(request, listener);
                }
            );
            nodeClient.admin().indices().create(cir, cirListener);
        }
    }

    private void createResource(CreateSampleResourceRequest request, ActionListener<CreateSampleResourceResponse> listener) {
        SharableResource sample = request.getResource();
        try {
            IndexRequest ir = nodeClient.prepareIndex(resourceIndex)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .setSource(sample.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS))
                .request();

            ActionListener<IndexResponse> irListener = ActionListener.wrap(idxResponse -> {
                listener.onResponse(new CreateSampleResourceResponse(idxResponse.getId()));
            }, listener::onFailure);
            nodeClient.index(ir, irListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
