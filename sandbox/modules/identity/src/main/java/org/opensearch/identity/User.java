/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.authn.StringPrincipal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A non-volatile and immutable object in the storage.
 *
 * @opensearch.experimental
 */

public class User {

    // Needed for InternalUsersStore.readUsersAsMap
    public User() {}

    /**
     * Create a new user
     *
     * @param username The username (must not be null or empty)
     * @param bcryptHash The hashed password (must not be null or empty)
     * @param attributes A map of custom attributes
     * @throws IllegalArgumentException if username or bcryptHash is null or empty
     */
    public User(final String username, final String bcryptHash, Map<String, String> attributes, List<String> permissions, List<String> backendRoles) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(bcryptHash);
        this.username = new StringPrincipal(username);
        this.bcryptHash = bcryptHash;
        this.attributes = attributes;
        this.permissions = permissions;
        this.backendRoles = backendRoles;
    }

    @JsonProperty(value = "username")
    private StringPrincipal username;

    @JsonProperty(value = "hash")
    private String bcryptHash;

    @JsonProperty(value = "attributes")
    private Map<String, String> attributes = Collections.emptyMap();

    @JsonProperty(value = "permissions")
    private List<String> permissions = Collections.emptyList();

    @JsonProperty(value = "backend_roles")
    private List<String> backendRoles = Collections.emptyList();

    @JsonProperty(value = "username")
    public StringPrincipal getUsername() {
        return username;
    }

    @JsonProperty(value = "username")
    public void setUsername(StringPrincipal username) {
        this.username = username;
    }

    @JsonProperty(value = "hash")
    public String getBcryptHash() {
        return bcryptHash;
    }

    @JsonProperty(value = "hash")
    public void setBcryptHash(String bcryptHash) {
        this.bcryptHash = bcryptHash;
    }

    @JsonProperty(value = "permissions")
    public List<String> getPermissions() {
        return permissions;
    }

    @JsonProperty(value = "permissions")
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @JsonProperty(value = "backend_roles")
    public List<String> getBackendRoles() {
        return backendRoles;
    }

    @JsonProperty(value = "backend_roles")
    public void setBackendRoles(List<String> backendRoles) {
        this.backendRoles = backendRoles;
    }

    @JsonProperty(value = "attributes")
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @JsonProperty(value = "attributes")
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "User [username=" + username + ", bcryptHash=" + bcryptHash + ", attributes=" + attributes + "]";
    }
}
