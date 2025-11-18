/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.rest;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.dashboards.action.AdvancedSettingsAction;
import org.opensearch.dashboards.action.AdvancedSettingsRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class RestAdvancedSettingsAction extends BaseRestHandler {

    @Override
    public String getName() {
        return "opensearch_dashboards_advanced_settings";
    }

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(GET, "/_opensearch_dashboards/advanced_settings/{index}"),
            new Route(PUT, "/_opensearch_dashboards/advanced_settings/{index}")
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String index = request.param("index");

        if (request.method() == PUT) {
            Map<String, Object> settings = null;
            if (request.hasContent()) {
                try (XContentParser parser = request.contentParser()) {
                    settings = parser.map();
                }
            }
            AdvancedSettingsRequest advancedSettingsRequest = new AdvancedSettingsRequest(index, settings);
            return channel -> client.execute(
                AdvancedSettingsAction.INSTANCE,
                advancedSettingsRequest,
                new RestToXContentListener<>(channel)
            );
        } else {
            AdvancedSettingsRequest advancedSettingsRequest = new AdvancedSettingsRequest(index, null);
            return channel -> client.execute(
                AdvancedSettingsAction.INSTANCE,
                advancedSettingsRequest,
                new RestToXContentListener<>(channel)
            );
        }
    }
}
