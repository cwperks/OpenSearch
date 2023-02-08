/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.rest.authinfo;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.List;

import static org.opensearch.rest.RestStatus.NOT_FOUND;
import static org.opensearch.rest.RestStatus.OK;

/**
 * Response class for getting logged in user info
 */
public class IdentityInfoResponse extends ActionResponse implements StatusToXContentObject {

    private String username;

    private List<String> backendRoles;

    public IdentityInfoResponse(String username, List<String> backendRoles) {
        this.username = username;
        this.backendRoles = backendRoles;
    }

    public IdentityInfoResponse(StreamInput in) throws IOException {
        super(in);
    }

    /**
     * @return Whether the attempt to Create a user was successful
     */
    @Override
    public RestStatus status() {
        if (username == null) return NOT_FOUND;
        return OK;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(username);
        out.writeStringArray(backendRoles.toArray(new String[0]));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("username", username);
        builder.field("backend_roles", backendRoles);
        builder.endObject();
        return builder;
    }
}
