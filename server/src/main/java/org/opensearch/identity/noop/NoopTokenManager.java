/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.noop;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.identity.TokenManager;
import org.opensearch.identity.tokens.AuthToken;

/**
 * Implementation of a TokenManager that gives back NoopTokens
 *
 * @opensearch.experimental
 */
public class NoopTokenManager implements TokenManager {
    @Override
    public AuthToken issueAccessTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) {
        return new NoopToken("");
    }

    @Override
    public AuthToken issueRefreshTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) {
        return new NoopToken("");
    }

    @Override
    public AuthToken generateServiceAccountToken(String extensionUniqueId) throws OpenSearchSecurityException {
        return new NoopToken("");
    }
}
