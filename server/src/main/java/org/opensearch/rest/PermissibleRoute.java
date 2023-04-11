/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest;

/**
 * A named Route
 *
 * @opensearch.api
 */
public class PermissibleRoute extends RestHandler.Route {

    private final String name;

    private final String legacyActionName;

    public PermissibleRoute(RestRequest.Method method, String path, String name) {
        super(method, path);
        this.name = name;
        this.legacyActionName = null;
    }

    public PermissibleRoute(RestRequest.Method method, String path, String name, String legacyActionName) {
        super(method, path);
        this.name = name;
        this.legacyActionName = legacyActionName;
    }

    /**
     * The name of the Route. Must be unique across Route.
     */
    public String name() {
        return this.name;
    }

    /**
     * The legacy action name of the Route. Must be unique across Route.
     */
    public String legacyActionName() {
        return this.legacyActionName;
    }
}
