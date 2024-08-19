/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.opensearch.identity.tokens.AuthToken;

import java.security.Principal;

public class SystemSubject implements Subject {
    private SystemPrincipal systemPrincipal = new SystemPrincipal();

    @Override
    public Principal getPrincipal() {
        return systemPrincipal;
    }

    @Override
    public void authenticate(AuthToken token) {
        // do nothing
    }
}
