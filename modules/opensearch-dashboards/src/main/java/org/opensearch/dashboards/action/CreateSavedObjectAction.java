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
 * Cluster-level action for creating a new saved object.
 * Authorization is a cluster permission — "can this user create resources of this type?"
 */
public class CreateSavedObjectAction extends ActionType<SavedObjectResponse> {

    public static final CreateSavedObjectAction INSTANCE = new CreateSavedObjectAction();
    public static final String NAME = "osd:saved_object/create";

    private CreateSavedObjectAction() {
        super(NAME, SavedObjectResponse::new);
    }
}
