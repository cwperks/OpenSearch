/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.http;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.threadpool.ThreadPool;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;

public class TestGetExecutionContextRestAction extends BaseRestHandler {

    private final ThreadPool threadPool;

    public TestGetExecutionContextRestAction(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_get_execution_context"));
    }

    @Override
    public String getName() {
        return "test_get_execution_context_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String pluginMainClass = threadPool.getThreadContext().getExecutionContext();
        RestResponse response = new BytesRestResponse(RestStatus.OK, pluginMainClass);
        return channel -> channel.sendResponse(response);
    }
}
