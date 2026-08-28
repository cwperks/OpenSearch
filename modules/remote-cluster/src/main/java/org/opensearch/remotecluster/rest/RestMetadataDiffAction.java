/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.rest;

import org.opensearch.remotecluster.action.MetadataDiffAction;
import org.opensearch.remotecluster.action.MetadataDiffRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * REST handler for the metadata diff API.
 */
public class RestMetadataDiffAction extends BaseRestHandler {

    @Override
    public String getName() {
        return "cross_cluster_metadata_diff";
    }

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(RestRequest.Method.GET, "/_remotes/{connection}/_metadata_diff")));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String connection = request.param("connection");
        String categoriesParam = request.param("categories", "");
        Set<String> categories;
        if (categoriesParam.isBlank()) {
            categories = Set.of("templates", "ingest_pipelines", "indices");
        } else {
            categories = Set.of(categoriesParam.split(","));
        }
        MetadataDiffRequest diffRequest = new MetadataDiffRequest(connection, categories);
        return channel -> client.execute(MetadataDiffAction.INSTANCE, diffRequest, new RestToXContentListener<>(channel));
    }
}
