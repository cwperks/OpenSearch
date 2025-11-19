/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.Version;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.time.DateFormatter;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TransportAdvancedSettingsAction extends HandledTransportAction<AdvancedSettingsRequest, AdvancedSettingsResponse> {

    private static final Logger log = LogManager.getLogger(TransportAdvancedSettingsAction.class);

    private final Client client;
    private final String configKey;

    @Inject
    public TransportAdvancedSettingsAction(TransportService transportService, ActionFilters actionFilters, Client client) {
        super(AdvancedSettingsAction.NAME, transportService, actionFilters, AdvancedSettingsRequest::new);
        this.client = client;
        this.configKey = "config:" + Version.CURRENT.toString();
    }

    @Override
    protected void doExecute(Task task, AdvancedSettingsRequest request, ActionListener<AdvancedSettingsResponse> listener) {
        try (final ThreadContext.StoredContext ctx = client.threadPool().getThreadContext().stashContext()) {
            if (request.getSettings() != null) {
                Map<String, Object> doc = Map.of(
                    "type",
                    "config",
                    "attributes",
                    request.getSettings(),
                    "references",
                    List.of(),
                    "updated_at",
                    DateFormatter.forPattern("strict_date_time").format(Instant.now())
                );

                log.info("Trying to index new UI Settings: " + doc);
                // PUT operation - store settings
                IndexRequest indexRequest = new IndexRequest(request.getIndex()).id(configKey).source(doc);

                client.index(
                    indexRequest,
                    ActionListener.wrap(
                        indexResponse -> listener.onResponse(new AdvancedSettingsResponse(request.getSettings())),
                        listener::onFailure
                    )
                );
            } else {
                // GET operation - retrieve settings
                GetRequest getRequest = new GetRequest(request.getIndex(), configKey);

                client.get(getRequest, ActionListener.wrap(getResponse -> {
                    if (getResponse.isExists()) {
                        listener.onResponse(new AdvancedSettingsResponse(getResponse.getSourceAsMap()));
                    } else {
                        listener.onResponse(new AdvancedSettingsResponse(Map.of()));
                    }
                }, listener::onFailure));
            }
        }
    }
}
