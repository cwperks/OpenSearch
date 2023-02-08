/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.rest.configuration;

import java.io.IOException;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.nodes.BaseNodesRequest;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

/**
 * Request to perform config update to reload configuration
 */
public class IdentityConfigUpdateRequest extends BaseNodesRequest<IdentityConfigUpdateRequest> {

    private String[] configTypes;

    public IdentityConfigUpdateRequest(StreamInput in) throws IOException {
        super(in);
        this.configTypes = in.readStringArray();
    }

    public IdentityConfigUpdateRequest() {
        super(new String[0]);
    }

    public IdentityConfigUpdateRequest(String[] configTypes) {
        this();
        setConfigTypes(configTypes);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(configTypes);
    }

    public String[] getConfigTypes() {
        return configTypes;
    }

    public void setConfigTypes(final String[] configTypes) {
        this.configTypes = configTypes;
    }

    @Override
    public ActionRequestValidationException validate() {
        if (configTypes == null || configTypes.length == 0) {
            return new ActionRequestValidationException();
        }
        return null;
    }
}
