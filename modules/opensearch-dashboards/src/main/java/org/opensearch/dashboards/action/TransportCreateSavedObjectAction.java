/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.action;

import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.dashboards.DashboardsPluginClient;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class TransportCreateSavedObjectAction extends HandledTransportAction<WriteSavedObjectRequest, SavedObjectResponse> {

    private final DashboardsPluginClient client;

    @Inject
    public TransportCreateSavedObjectAction(TransportService transportService, ActionFilters actionFilters,
                                            DashboardsPluginClient client) {
        super(CreateSavedObjectAction.NAME, transportService, actionFilters, WriteSavedObjectRequest::new);
        this.client = client;
    }

    @Override
    protected void doExecute(Task task, WriteSavedObjectRequest request, ActionListener<SavedObjectResponse> listener) {
        IndexRequest indexRequest = new IndexRequest(request.getIndex())
            .id(request.getDocumentId())
            .source(request.getDocument())
            .create(true);

        client.index(indexRequest, ActionListener.wrap(
            indexResponse -> listener.onResponse(new SavedObjectResponse(request.getDocument())),
            listener::onFailure
        ));
    }
}
