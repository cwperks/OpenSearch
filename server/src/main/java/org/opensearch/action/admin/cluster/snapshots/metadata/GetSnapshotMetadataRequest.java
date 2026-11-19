/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.cluster.snapshots.metadata;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import org.opensearch.common.annotation.ExperimentalApi;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;

import static org.opensearch.action.ValidateActions.addValidationError;

/** Identifies one repository and one completed snapshot; patterns are not supported. */
@ExperimentalApi
public class GetSnapshotMetadataRequest extends ClusterManagerNodeRequest<GetSnapshotMetadataRequest> {
    private final String repository;
    private final String snapshot;

    public GetSnapshotMetadataRequest(String repository, String snapshot) {
        this.repository = repository;
        this.snapshot = snapshot;
    }

    public GetSnapshotMetadataRequest(StreamInput in) throws IOException {
        super(in);
        repository = in.readString();
        snapshot = in.readString();
    }

    public String repository() {
        return repository;
    }

    public String snapshot() {
        return snapshot;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException error = null;
        if (Strings.hasText(repository) == false) {
            error = addValidationError("repository is missing", error);
        }
        if (Strings.hasText(snapshot) == false) {
            error = addValidationError("snapshot is missing", error);
        }
        return error;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(repository);
        out.writeString(snapshot);
    }
}
