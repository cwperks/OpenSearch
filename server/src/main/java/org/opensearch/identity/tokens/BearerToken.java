/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.tokens;

/**
 * Class that represents a token used for HTTP Bearer Authentication
 *
 * Bearer tokens are passed in the HTTP Authorization header
 *
 * Authorization: Bearer {bearer_token}
 *
 * @opensearch.experimental
 */
public class BearerToken extends AuthToken {
    public BearerToken(String tokenValue) {
        super(tokenValue);
    }
}
