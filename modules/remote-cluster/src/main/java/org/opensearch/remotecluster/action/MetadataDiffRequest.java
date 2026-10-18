/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.action;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.ValidateActions;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Set;

/**
 * Request for metadata diff between local and remote clusters.
 */
public class MetadataDiffRequest extends ActionRequest {
    private final String connectionName;
    private final Set<String> categories;

    public MetadataDiffRequest(String connectionName, Set<String> categories) {
        this.connectionName = connectionName;
        this.categories = categories;
    }

    public MetadataDiffRequest(StreamInput in) throws IOException {
        super(in);
        this.connectionName = in.readString();
        this.categories = in.readSet(StreamInput::readString);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(connectionName);
        out.writeStringCollection(categories);
    }

    public String getConnectionName() {
        return connectionName;
    }

    public Set<String> getCategories() {
        return categories;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (connectionName == null || connectionName.isBlank()) {
            validationException = ValidateActions.addValidationError("connection_name must not be empty", validationException);
        }
        return validationException;
    }
}
