/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.rest.authinfo;

import org.opensearch.client.node.NodeClient;
import org.opensearch.identity.rest.IdentityRestConstants;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;

import java.util.List;

import static java.util.Arrays.asList;
import static org.opensearch.identity.utils.RoutesHelper.addRoutesPrefix;
import static org.opensearch.rest.RestRequest.Method.GET;

/**
 * Rest action for getting information about the logged in user
 *
 * /authinfo rest request action to get information about the logged in user
 *
 * @opensearch.api
 */
public class RestIdentityInfoAction extends BaseRestHandler {

    @Override
    public String getName() {
        return IdentityRestConstants.IDENTITY_AUTHINFO_ACTION;
    }

    /**
     * Rest request handler for creating a new user
     * @param request the request to execute
     * @param client  client for executing actions on the local node
     * @return the action to be executed See {@link #handleRequest(RestRequest, RestChannel, NodeClient) for more}
     *
     * ````
     * Sample Request:
     * curl -XGET http://new-user:password@localhost:9200/_identity/api/authinfo
     *
     *
     * Sample Response
     *
     * {
     *   "user": "User [name=admin, backend_roles=[admin], requestedTenant=null]",
     *   "user_name": "admin",
     *   "user_requested_tenant": null,
     *   "remote_address": "172.25.0.1:65382",
     *   "backend_roles": [
     *     "admin"
     *   ],
     *   "custom_attribute_names": [],
     *   "roles": [
     *     "own_index",
     *     "all_access"
     *   ],
     *   "tenants": {
     *     "global_tenant": true,
     *     "admin_tenant": true,
     *     "admin": true
     *   },
     *   "principal": null,
     *   "peer_certificates": "0",
     *   "sso_logout_url": null
     * }
     */
    @Override
    @SuppressWarnings("unchecked")
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        IdentityInfoRequest identityInfoRequest = new IdentityInfoRequest();
        return channel -> client.doExecute(IdentityInfoAction.INSTANCE, identityInfoRequest, new RestStatusToXContentListener<>(channel));
    }

    /**
     * Routes to be registered for this action
     * @return the unmodifiable list of routes to be registered
     */
    @Override
    public List<Route> routes() {
        // e.g. return value "_identity/api/authinfo"
        return addRoutesPrefix(asList(new Route(GET, "/authinfo")));
    }

}
