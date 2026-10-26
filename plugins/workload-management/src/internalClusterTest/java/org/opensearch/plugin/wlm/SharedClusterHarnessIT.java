/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.wlm;

import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.opensearch.Version;
import org.opensearch.client.Request;
import org.opensearch.client.RestClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.rule.RuleFrameworkPlugin;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.cluster.LocalTestCluster;
import org.opensearch.transport.Netty4ModulePlugin;

import java.util.List;

/** Exercises production WLM plugin wiring using the composable shared fixture. */
public class SharedClusterHarnessIT extends OpenSearchTestCase {
    public void testWorkloadGroupThroughHttp() throws Exception {
        List<PluginInfo> plugins = List.of(
            plugin("transport-netty4", Netty4ModulePlugin.class),
            plugin("rule-framework", RuleFrameworkPlugin.class),
            plugin("workload-management", WorkloadManagementPlugin.class, "rule-framework")
        );
        Settings settings = Settings.builder()
            .put("http.type", "netty4")
            .put("transport.type", "netty4")
            .put("node.processors", 1)
            .put("wlm.workload_group.mode", "enabled")
            .build();
        try (
            LocalTestCluster cluster = new LocalTestCluster(
                "wlm-shared-harness",
                List.of(new LocalTestCluster.NodeSpec(List.of("cluster_manager", "data"), plugins)),
                ignored -> settings,
                null
            )
        ) {
            cluster.start();
            expectThrows(IllegalStateException.class, cluster::start);
            try (
                RestClient client = RestClient.builder(new HttpHost("http", "127.0.0.1", cluster.clientNode().getHttpAddress().getPort()))
                    .build()
            ) {
                Request create = new Request("PUT", "/_wlm/workload_group");
                create.setJsonEntity("""
                    {"name":"shared-harness","resiliency_mode":"monitor","resource_limits":{"cpu":0.4,"memory":0.4}}
                    """);
                assertEquals(200, client.performRequest(create).getStatusLine().getStatusCode());
                String groups = EntityUtils.toString(client.performRequest(new Request("GET", "/_wlm/workload_group")).getEntity());
                assertTrue(groups, groups.contains("shared-harness"));
            }
            cluster.close();
            assertFalse(cluster.isStarted());
        }
    }

    private static PluginInfo plugin(String name, Class<? extends Plugin> type, String... extensions) {
        return new PluginInfo(name, "test", "test", Version.CURRENT, "21", type.getName(), null, List.of(extensions), false);
    }
}
