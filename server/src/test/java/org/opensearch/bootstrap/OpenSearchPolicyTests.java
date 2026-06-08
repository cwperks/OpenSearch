/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.bootstrap;

import org.opensearch.secure_sm.policy.Policy;
import org.opensearch.test.OpenSearchTestCase;

import java.net.SocketPermission;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Map;

public class OpenSearchPolicyTests extends OpenSearchTestCase {

    public void testFilterBadDefaultsBlocksStopThreadPermission() throws Exception {
        Permission permission = new RuntimePermission("stopThread");

        assertFalse(newPolicy(true).implies(domainWithStaticPermission(permission), permission));
        assertTrue(newPolicy(false).implies(domainWithStaticPermission(permission), permission));
    }

    public void testFilterBadDefaultsBlocksLocalhostDynamicListenPermission() throws Exception {
        Permission permission = new SocketPermission("localhost:0", "listen");

        assertFalse(newPolicy(true).implies(domainWithStaticPermission(permission), permission));
        assertTrue(newPolicy(false).implies(domainWithStaticPermission(permission), permission));
    }

    public void testFilterBadDefaultsAllowsOtherFallbackPermissions() throws Exception {
        Permission permission = new RuntimePermission("getClassLoader");

        assertTrue(newPolicy(true).implies(domainWithStaticPermission(permission), permission));
    }

    public void testFilterBadDefaultsAllowsExplicitPluginPolicyGrant() throws Exception {
        Permission permission = new RuntimePermission("stopThread");
        URL pluginUrl = new URL("file:/plugin.jar");
        ProtectionDomain pluginDomain = domainWithStaticPermission(pluginUrl, permission);
        Policy pluginPolicy = new Policy() {
            @Override
            public boolean implies(ProtectionDomain domain, Permission requestedPermission) {
                return domain.getCodeSource().getLocation().equals(pluginUrl) && requestedPermission.equals(permission);
            }
        };

        assertTrue(newPolicy(true, Map.of(pluginUrl.getFile(), pluginPolicy)).implies(pluginDomain, permission));
    }

    private static OpenSearchPolicy newPolicy(boolean filterBadDefaults) {
        return newPolicy(filterBadDefaults, Collections.emptyMap());
    }

    private static OpenSearchPolicy newPolicy(boolean filterBadDefaults, Map<String, Policy> plugins) {
        return new OpenSearchPolicy(Collections.emptyMap(), new Permissions(), plugins, filterBadDefaults, new Permissions());
    }

    private static ProtectionDomain domainWithStaticPermission(Permission permission) throws Exception {
        return domainWithStaticPermission(new URL("file:/test.jar"), permission);
    }

    private static ProtectionDomain domainWithStaticPermission(URL location, Permission permission) {
        Permissions permissions = new Permissions();
        permissions.add(permission);
        return new ProtectionDomain(new CodeSource(location, (Certificate[]) null), permissions);
    }
}
