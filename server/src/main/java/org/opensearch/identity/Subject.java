/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.identity;

import java.security.Principal;

/**
 * An individual, process, or device that causes information to flow among objects or change to the system state.
 *
 * @opensearch.experimental
 */
public interface Subject {

    /**
     * Get the application-wide uniquely identifying principal
     * */
    Principal getPrincipal();

    /**
     * Login through an authentication token
     * throws UnsupportedAuthenticationMethod
     * throws InvalidAuthenticationToken
     * throws SubjectNotFound
     * throws SubjectDisabled
     */
    // TODO Uncomment login and make a richer interface to IdentityPlugin for authc and authz use-cases
    // void login(final AuthToken token);

    /**
     * Method that returns whether the current subject of the running thread is authenticated
     */
    boolean isAuthenticated();
}
