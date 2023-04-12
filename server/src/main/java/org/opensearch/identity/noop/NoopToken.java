/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.noop;

import org.opensearch.identity.tokens.AuthToken;

/**
 * Implementation of an AuthToken that gives back an empty token
 *
 * @opensearch.experimental
 */
public class NoopToken extends AuthToken {
    public NoopToken(String tokenValue) {
        super(tokenValue);
    }
}
