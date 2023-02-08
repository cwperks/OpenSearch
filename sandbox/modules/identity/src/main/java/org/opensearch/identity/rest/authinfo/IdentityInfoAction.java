/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.rest.authinfo;

import org.opensearch.action.ActionType;

public class IdentityInfoAction extends ActionType<IdentityInfoResponse> {

    public static final IdentityInfoAction INSTANCE = new IdentityInfoAction();

    public static final String NAME = "identity:authinfo";

    private IdentityInfoAction() {
        super(NAME, IdentityInfoResponse::new);
    }
}
