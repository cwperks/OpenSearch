/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.cluster.snapshots.metadata;

import org.opensearch.action.ActionType;
import org.opensearch.common.annotation.ExperimentalApi;

/** Read-only inventory of a snapshot's cluster metadata. */
@ExperimentalApi
public class GetSnapshotMetadataAction extends ActionType<GetSnapshotMetadataResponse> {
    public static final String NAME = "cluster:admin/snapshot/metadata/get";
    public static final GetSnapshotMetadataAction INSTANCE = new GetSnapshotMetadataAction();

    private GetSnapshotMetadataAction() {
        super(NAME, GetSnapshotMetadataResponse::new);
    }
}
