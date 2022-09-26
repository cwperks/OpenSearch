/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.extensions.rest;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.identity.PrincipalIdentifierToken;
import org.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Request to evaluate user privileges for extension actions
 *
 * @opensearch.internal
 */
public class AuthorizationRequest extends TransportRequest {

    private String extensionUniqueId;
    private PrincipalIdentifierToken requestIssuerIdentity;
    private String permissionId;
    private Map<String, Object> params;

    public AuthorizationRequest(String extensionUniqueId, PrincipalIdentifierToken requestIssuerIdentity, String permissionId, Map<String, Object> params) {
        this.extensionUniqueId = extensionUniqueId;
        this.requestIssuerIdentity = requestIssuerIdentity;
        this.permissionId = permissionId;
        this.params = params;
    }

    public AuthorizationRequest(StreamInput in) throws IOException {
        super(in);
        extensionUniqueId = in.readString();
        requestIssuerIdentity = in.readNamedWriteable(PrincipalIdentifierToken.class);
        permissionId = in.readString();
        if (in.readBoolean()) {
            params = in.readMap();
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(extensionUniqueId);
        out.writeNamedWriteable(requestIssuerIdentity);
        out.writeString(permissionId);
        boolean hasParams = params != null;
        out.writeBoolean(hasParams);
        if (hasParams) {
            out.writeMap(params);
        }
    }

    public String getExtensionUniqueId() {
        return extensionUniqueId;
    }

    public PrincipalIdentifierToken getRequestIssuerIdentity() {
        return requestIssuerIdentity;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "AuthorizationRequest{extensionUniqueId=" + extensionUniqueId + ", requestIssuerIdentity=" + requestIssuerIdentity + ", permissionId=" + permissionId + ", params=" + params + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AuthorizationRequest that = (AuthorizationRequest) obj;
        return Objects.equals(extensionUniqueId, that.extensionUniqueId) && Objects.equals(requestIssuerIdentity, that.requestIssuerIdentity) && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensionUniqueId, requestIssuerIdentity, permissionId);
    }
}
