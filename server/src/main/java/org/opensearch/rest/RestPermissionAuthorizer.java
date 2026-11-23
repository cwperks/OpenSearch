/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.rest;

import org.opensearch.common.annotation.ExperimentalApi;

/**
 * Authorizes a route's cluster permission before its handler executes.
 * Implementations must reject unauthenticated or unauthorized callers by throwing an exception.
 * Returning normally grants access. This check does not replace downstream index or resource authorization.
 *
 * @opensearch.experimental
 */
@ExperimentalApi
@FunctionalInterface
public interface RestPermissionAuthorizer {
    /**
     * Checks the authenticated caller's permission. Runs inside the REST handler wrapper,
     * which must authenticate the request before forwarding it.
     *
     * @param request the incoming request
     * @param permission the route's cluster permission
     * @throws Exception if authorization cannot be granted
     */
    void authorize(RestRequest request, String permission) throws Exception;
}
