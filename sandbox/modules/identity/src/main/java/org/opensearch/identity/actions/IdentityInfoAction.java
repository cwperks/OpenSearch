/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.threadpool.ThreadPool;

public class IdentityInfoAction extends BaseRestHandler {
    // private static final List<Route> routes = addRoutesPrefix(List.of(
    // new Route(GET, "/authinfo"),
    // new Route(POST, "/authinfo")
    // ),"/_opendistro/_security", "/_plugins/_security");

    private final Logger log = LogManager.getLogger(this.getClass());
    private final ThreadContext threadContext;

    public IdentityInfoAction(final Settings settings, final RestController controller, final ThreadPool threadPool) {
        super();
        this.threadContext = threadPool.getThreadContext();
    }

    @Override
    public List<Route> routes() {
        // return routes;
        return List.of();
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return new RestChannelConsumer() {

            @Override
            public void accept(RestChannel channel) throws Exception {
                XContentBuilder builder = channel.newBuilder(); // NOSONAR
                BytesRestResponse response = null;

                try {

                    // final User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);

                    builder.startObject();
                    // builder.field("user", user==null?null:user.toString());
                    // builder.field("user_name", user==null?null:user.getName());
                    // builder.field("user_requested_tenant", (String)null);
                    // builder.field("remote_address", (String)null);
                    // builder.field("backend_roles", user==null?null:user.getRoles());
                    // builder.field("custom_attribute_names", user==null?null:user.getCustomAttributesMap().keySet());
                    builder.field("roles", (Set<String>) null);
                    builder.field("tenants", (Map<String, Boolean>) null);
                    builder.field("principal", (String) null);
                    builder.field("peer_certificates", (String) null);
                    builder.field("sso_logout_url", (String) null);

                    builder.endObject();

                    response = new BytesRestResponse(RestStatus.OK, builder);
                } catch (final Exception e1) {
                    log.error(e1.toString(), e1);
                    builder = channel.newBuilder(); // NOSONAR
                    builder.startObject();
                    builder.field("error", e1.toString());
                    builder.endObject();
                    response = new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, builder);
                } finally {
                    if (builder != null) {
                        builder.close();
                    }
                }

                channel.sendResponse(response);
            }
        };
    }

    @Override
    public String getName() {
        return "OpenSearch Identity Info Action";
    }
}
