/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.extensions.rest;

import java.util.function.Function;

import org.opensearch.rest.RestHandler.Route;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestRequest.Method;

/**
 * A subclass of {@link Route} that includes a handler method for that route.
 */
public class RouteHandler extends Route {

    private final String name;

    private final String legacyActionName;

    private final Function<RestRequest, ExtensionRestResponse> responseHandler;

    /**
     * Handle the method and path with the specified handler.
     *
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the method and path.
     */
    public RouteHandler(Method method, String path, Function<RestRequest, ExtensionRestResponse> handler) {
        super(method, path);
        this.responseHandler = handler;
        this.name = null;
        this.legacyActionName = null;
    }

    /**
     * Handle the method and path with the specified handler.
     *
     * @param name The name of the handler.
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the method and path.
     */
    public RouteHandler(String name, Method method, String path, Function<RestRequest, ExtensionRestResponse> handler) {
        super(method, path);
        this.responseHandler = handler;
        this.name = name;
        this.legacyActionName = null;
    }

    /**
     * Handle the method and path with the specified handler.
     *
     * @param name The name of the handler.
     * @param legacyActionName The legacy action name corresponding to this handler.
     * @param method The {@link Method} to handle.
     * @param path The path to handle.
     * @param handler The method which handles the method and path.
     */
    @Deprecated
    public RouteHandler(
        String name,
        String legacyActionName,
        Method method,
        String path,
        Function<RestRequest, ExtensionRestResponse> handler
    ) {
        super(method, path);
        this.responseHandler = handler;
        this.name = name;
        this.legacyActionName = legacyActionName;
    }

    /**
     * Executes the handler for this route.
     *
     * @param request The request to handle
     * @return the {@link ExtensionRestResponse} result from the handler for this route.
     */
    public ExtensionRestResponse handleRequest(RestRequest request) {
        return responseHandler.apply(request);
    }

    /**
     * The name of the RouteHandler. Must be unique across route handlers.
     */
    public String name() {
        return this.name;
    }

    /**
     * The legacy action name corresponding to the RouteHandler. Must be unique across route handlers.
     */
    public String legacyActionName() {
        return this.legacyActionName;
    }
}
