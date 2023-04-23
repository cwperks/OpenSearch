/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.opensearch.OpenSearchSecurityException;
import org.opensearch.identity.tokens.AuthToken;

/**
 * Interface for a token manager to issue Auth Tokens for a User
 *
 * @opensearch.experimental
 */
public interface TokenManager {
    /**
     * Issue an access token on-behalf-of authenticated user for a service to utilize to interact with
     * the OpenSearch cluster on-behalf-of the original user
     * */
    AuthToken issueAccessTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) throws OpenSearchSecurityException;

    /**
     * Issue a refresh token on-behalf-of authenticated user for a service to refresh access tokens
     * */
    AuthToken issueRefreshTokenOnBehalfOfAuthenticatedUser(String extensionUniqueId) throws OpenSearchSecurityException;

    /**
     * Issue a service account token for an extension's service account
     * */
    AuthToken generateServiceAccountToken(String extensionUniqueId) throws OpenSearchSecurityException;
}
