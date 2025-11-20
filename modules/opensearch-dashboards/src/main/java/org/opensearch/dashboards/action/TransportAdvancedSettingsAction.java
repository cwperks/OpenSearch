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
import org.opensearch.OpenSearchStatusException;
import org.opensearch.Version;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.time.DateFormatter;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import java.time.Instant;
import java.util.HashMap;
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
                // First get existing document to merge settings
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
                    existingConfig.putAll(request.getSettings());

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

                    log.info("Trying to index merged UI Settings: " + doc);
                    IndexRequest indexRequest = new IndexRequest(request.getIndex()).id(configKey).source(doc);

                    client.index(
                        indexRequest,
                        ActionListener.wrap(
                            indexResponse -> listener.onResponse(new AdvancedSettingsResponse(doc)),
                            (e) -> {
                                System.out.println("Caught error trying to index merged UI Settings. " + e.getMessage());
                                log.warn("Caught error trying to index merged UI Settings", e);
                                listener.onFailure(e);
                            }
                        )
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

                        log.info("Index not found, creating new UI Settings: " + doc);
                        IndexRequest indexRequest = new IndexRequest(request.getIndex()).id(configKey).source(doc);

                        client.index(
                            indexRequest,
                            ActionListener.wrap(
                                indexResponse -> listener.onResponse(new AdvancedSettingsResponse(doc)),
                                (e2) -> {
                                    System.out.println("Caught error trying to index new UI Settings. " + e2.getMessage());
                                    log.warn("Caught error trying to index new UI Settings", e2);
                                    listener.onFailure(e2);
                                }
                            )
                        );
                    } else {
                        System.out.println("Caught error when getting existing settings. " + e.getMessage());
                        log.warn("Caught error when getting existing settings", e);
                        listener.onFailure(e);
                    }
                }));
            } else {
                // GET operation - retrieve settings
                GetRequest getRequest = new GetRequest(request.getIndex(), configKey);

                client.get(getRequest, ActionListener.wrap(getResponse -> {
                    if (getResponse.isExists()) {
                        Map<String, Object> source = getResponse.getSourceAsMap();
                        listener.onResponse(new AdvancedSettingsResponse(source));
                    } else {
                        listener.onFailure(new OpenSearchStatusException("Advanced settings not found", RestStatus.NOT_FOUND));
                    }
                }, (e) -> {
                    if (e instanceof IndexNotFoundException) {
                        listener.onFailure(new OpenSearchStatusException("Advanced settings not found", RestStatus.NOT_FOUND));
                    } else {
                        listener.onFailure(e);
                    }
                }));
            }
        }
    }
}
