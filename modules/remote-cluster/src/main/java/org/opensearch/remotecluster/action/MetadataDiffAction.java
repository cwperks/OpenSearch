/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.action;

import org.opensearch.action.ActionType;

/**
 * Action type for metadata diff between local and remote clusters.
 */
public class MetadataDiffAction extends ActionType<MetadataDiffResponse> {
    public static final MetadataDiffAction INSTANCE = new MetadataDiffAction();
    public static final String NAME = "cluster:admin/remotes/metadata/diff";

    private MetadataDiffAction() {
        super(NAME, MetadataDiffResponse::new);
    }
}
