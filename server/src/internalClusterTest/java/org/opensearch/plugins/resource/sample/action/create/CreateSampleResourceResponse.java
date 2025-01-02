/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.sample.action.create;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

/**
 * Response to a CreateSampleResourceRequest
 */
public class CreateSampleResourceResponse extends ActionResponse implements ToXContentObject {
    private final String resourceId;

    /**
     * Default constructor
     *
     * @param resourceId The resourceId
     */
    public CreateSampleResourceResponse(String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(resourceId);
    }

    /**
     * Constructor with StreamInput
     *
     * @param in the stream input
     */
    public CreateSampleResourceResponse(final StreamInput in) throws IOException {
        resourceId = in.readString();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("resourceId", resourceId);
        builder.endObject();
        return builder;
    }
}
