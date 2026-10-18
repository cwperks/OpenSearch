/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache 2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.ActionType;

/**
 * Transport action to list index mappings in pages.
 *
 * @opensearch.internal
 */
public class ListMappingsAction extends ActionType<ListMappingsResponse> {

    public static final ListMappingsAction INSTANCE = new ListMappingsAction();
    public static final String NAME = "indices:admin/mappings/list";

    private ListMappingsAction() {
        super(NAME, ListMappingsResponse::new);
    }
}
