/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.rest;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionType;
import org.opensearch.common.annotation.ExperimentalApi;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * A REST handler whose routes all dispatch through one registered action.
 * The declared action is the action executed through the client, including its action filters.
 * This is an alternative to a {@link NamedRoute}'s REST-level permission check.
 *
 * <p>Parsing must only construct the action request: protected reads and side effects belong
 * in action execution. The framework does not clear the caller's thread context. This convention
 * does not itself supply an authorization policy or replace downstream index/resource checks.</p>
 *
 * @opensearch.experimental
 */
@ExperimentalApi
public abstract class BaseRestActionHandler<Request extends ActionRequest, Response extends ActionResponse & ToXContentObject> extends
    BaseRestHandler {
    private final ActionType<Response> action;
    private final List<Route> routes;

    /**
     * Declares the action executed by every route of this handler.
     *
     * @param action a registered action
     * @param routes the HTTP routes mapped to that action
     */
    protected BaseRestActionHandler(ActionType<Response> action, Route... routes) {
        this.action = Objects.requireNonNull(action, "action");
        this.routes = List.of(routes);
        if (this.routes.isEmpty()) {
            throw new IllegalArgumentException("An action-backed handler must declare at least one route");
        }
        if (this.routes.stream().anyMatch(route -> route instanceof NamedRoute)) {
            throw new IllegalArgumentException("Use ordinary routes for an action-backed handler");
        }
    }

    /** Returns the entry-point action executed for this handler's routes. */
    public final ActionType<Response> action() {
        return action;
    }

    @Override
    public final List<Route> routes() {
        return routes;
    }

    @Override
    protected final RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        Request actionRequest = Objects.requireNonNull(parseRequest(request), "action request");
        return channel -> client.execute(action, actionRequest, responseListener(channel));
    }

    /**
     * Parses parameters and content into an action request, without executing protected operations.
     *
     * @param request the incoming REST request
     * @return the request to dispatch through the declared action
     * @throws IOException if request parsing fails
     */
    protected abstract Request parseRequest(RestRequest request) throws IOException;

    /**
     * Creates the response serializer. Override for custom status codes or response formatting.
     *
     * @param channel the response channel
     * @return the listener for the action result
     */
    protected ActionListener<Response> responseListener(RestChannel channel) {
        return new RestToXContentListener<>(channel);
    }
}
