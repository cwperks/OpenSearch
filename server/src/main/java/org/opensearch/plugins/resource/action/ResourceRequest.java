/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.action;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;

public class ResourceRequest extends ActionRequest {
    protected final String resourceIndex;

    /**
     * Default constructor
     */
    public ResourceRequest(String resourceIndex) {
        this.resourceIndex = resourceIndex;
    }

    public ResourceRequest(StreamInput in) throws IOException {
        this.resourceIndex = in.readString();
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(resourceIndex);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getResourceIndex() {
        return this.resourceIndex;
    }
}
