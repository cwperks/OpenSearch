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

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class AuthorizationRestFilter {

    private final Logger log = LogManager.getLogger(this.getClass());

    private List<UnaryOperator<RestHandler>> restWrappers = new ArrayList<>();

    public AuthorizationRestFilter(List<UnaryOperator<RestHandler>> restWrappers) {
        this.restWrappers = restWrappers;
    }

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
                    RestChannel originalChannel = noopChannel.getOriginalChannel();
                    List<RestResponse> capturedResponses = noopChannel.capturedResponses();
                    System.out.println("capturedResponses: " + noopChannel.capturedResponses());
                    // All authentication REST filters failed
                    if (restWrappers.size() > 0 && restWrappers.size() == capturedResponses.size()) {
                        // TODO Eagerly responding with first failure response, how should this respond?
                        originalChannel.sendResponse(capturedResponses.get(0));
                        return;
                    }
                    // At least one authentication REST filter succeeded
                    original.handleRequest(request, originalChannel, client);
                } else {
                    original.handleRequest(request, channel, client);
                }
            }
        };
    }
}

