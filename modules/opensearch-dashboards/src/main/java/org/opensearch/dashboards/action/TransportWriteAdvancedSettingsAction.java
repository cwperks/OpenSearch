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
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportWriteAdvancedSettingsAction extends HandledTransportAction<WriteAdvancedSettingsRequest, AdvancedSettingsResponse> {

    private static final Logger log = LogManager.getLogger(TransportWriteAdvancedSettingsAction.class);

    private final Client client;
    private final String configKey;

    @Inject
    public TransportWriteAdvancedSettingsAction(TransportService transportService, ActionFilters actionFilters, Client client) {
        super(WriteAdvancedSettingsAction.NAME, transportService, actionFilters, WriteAdvancedSettingsRequest::new);
        this.client = client;
        this.configKey = "config:" + Version.CURRENT.toString();
    }

    @Override
    protected void doExecute(Task task, WriteAdvancedSettingsRequest request, ActionListener<AdvancedSettingsResponse> listener) {
        try (final ThreadContext.StoredContext ctx = client.threadPool().getThreadContext().stashContext()) {
            GetRequest getRequest = new GetRequest(request.getIndex(), configKey);

            client.get(getRequest, ActionListener.wrap(getResponse -> {
                Map<String, Object> existingConfig = new HashMap<>();

                if (getResponse.isExists()) {
                    Map<String, Object> source = getResponse.getSourceAsMap();
                    Object config = source.get("config");
                    if (config instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> configMap = (Map<String, Object>) config;
                        existingConfig.putAll(configMap);
                    }
                }

                // Merge new settings with existing ones
                Object newConfigObj = request.getSettings().getOrDefault("config", Collections.emptyMap());
                if (newConfigObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> newConfig = (Map<String, Object>) newConfigObj;
                    existingConfig.putAll(newConfig);
                }

                Map<String, Object> doc = Map.of(
                    "type",
                    "config",
                    "config",
                    existingConfig,
                    "references",
                    List.of(),
                    "updated_at",
                    DateFormatter.forPattern("strict_date_time").format(Instant.now())
                );

                IndexRequest indexRequest = new IndexRequest(request.getIndex()).id(configKey).source(doc);

                client.index(
                    indexRequest,
                    ActionListener.wrap(indexResponse -> listener.onResponse(new AdvancedSettingsResponse(doc)), (e) -> {
                        System.out.println("Received error when indexing merged doc: " + e.getMessage());
                        listener.onFailure(e);
                    })
                );
            }, (e) -> {
                if (e instanceof IndexNotFoundException) {
                    // Index doesn't exist, proceed with new settings only
                    Map<String, Object> doc = Map.of(
                        "type",
                        "config",
                        "config",
                        request.getSettings(),
                        "references",
                        List.of(),
                        "updated_at",
                        DateFormatter.forPattern("strict_date_time").format(Instant.now())
                    );

                    IndexRequest indexRequest = new IndexRequest(request.getIndex()).id(configKey).source(doc);

                    client.index(
                        indexRequest,
                        ActionListener.wrap(indexResponse -> listener.onResponse(new AdvancedSettingsResponse(doc)), (e2) -> {
                            System.out.println("Received error when index doc: " + e2.getMessage());
                            listener.onFailure(e2);
                        })
                    );
                } else {
                    listener.onFailure(e);
                }
            }));
        }
    }
}
