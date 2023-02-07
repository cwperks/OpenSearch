/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.node.NodeClient;
import org.opensearch.http.NoopResponseCollectingRestChannel;

public class AuthorizationRestFilter {

    protected final Logger log = LogManager.getLogger(this.getClass());

    /**
     * This function wraps around all rest requests
     * If the request is authenticated, then it goes through
     */
    public RestHandler wrap(RestHandler original) {
        return new RestHandler() {

            @Override
            public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                System.out.println("AuthorizationRestFilter.wrap");
                if (channel instanceof NoopResponseCollectingRestChannel) {
                    NoopResponseCollectingRestChannel noopChannel = (NoopResponseCollectingRestChannel)channel;
                    System.out.println("capturedResponses: " + noopChannel.capturedResponses());
                    original.handleRequest(request, noopChannel.getOriginalChannel(), client);
                } else {
                    original.handleRequest(request, channel, client);
                }
            }
        };
    }
}

