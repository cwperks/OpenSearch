/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.action;

import org.opensearch.action.ActionType;

/**
 * Resource-level action for updating an existing saved object.
 * Authorization checks the user's permission on the specific resource via sharing records / workspace membership.
 */
public class UpdateSavedObjectAction extends ActionType<SavedObjectResponse> {

    public static final UpdateSavedObjectAction INSTANCE = new UpdateSavedObjectAction();
    public static final String NAME = "osd:saved_object/update";

    private UpdateSavedObjectAction() {
        super(NAME, SavedObjectResponse::new);
    }
}
