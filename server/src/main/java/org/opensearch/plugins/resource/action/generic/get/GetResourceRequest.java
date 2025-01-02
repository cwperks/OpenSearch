/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.action.generic.get;

import org.opensearch.action.ResourceRequest;
import org.opensearch.core.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * Request object for GetResource transport action
 */
public class GetResourceRequest extends ResourceRequest {
    private final String resourceId;

    /**
     * Default constructor
     */
    public GetResourceRequest(String resourceId, String resourceIndex) {
        super(resourceIndex);
        this.resourceId = resourceId;
    }

    public GetResourceRequest(StreamInput in) throws IOException {
        super(in);
        this.resourceId = in.readString();
    }

    public String getResourceId() {
        return this.resourceId;
    }
}
