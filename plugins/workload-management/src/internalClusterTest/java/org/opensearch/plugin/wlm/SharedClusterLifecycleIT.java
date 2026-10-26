/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.plugin.wlm;

import org.opensearch.Version;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.cluster.LocalTestCluster;
import org.opensearch.transport.Netty4ModulePlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Lifecycle coverage independent of WLM's static feature registry. */
public class SharedClusterLifecycleIT extends OpenSearchTestCase {
    private static final List<PluginInfo> PLUGINS = List.of(
        new PluginInfo(
            "transport-netty4",
            "test",
            "test",
            Version.CURRENT,
            "21",
            Netty4ModulePlugin.class.getName(),
            null,
            List.of(),
            false
        )
    );
    private static final Settings SETTINGS = Settings.builder()
        .put("http.type", "netty4")
        .put("transport.type", "netty4")
        .put("network.host", "127.0.0.1")
        .put("node.processors", 1)
        .build();

    public void testMultiNodeRestartAndCleanup() throws Exception {
        Path home;
        try (
            LocalTestCluster cluster = new LocalTestCluster(
                "shared-multi-node",
                List.of(
                    new LocalTestCluster.NodeSpec(List.of("cluster_manager"), PLUGINS),
                    new LocalTestCluster.NodeSpec(List.of("data"), PLUGINS),
                    new LocalTestCluster.NodeSpec(List.of(), PLUGINS)
                ),
                ignored -> SETTINGS,
                null
            )
        ) {
            home = Path.of(cluster.getSnapshotDirPath()).getParent();
            cluster.start();
            assertEquals(3, cluster.getNodes().size());
            assertEquals("client_0", cluster.clientNode().getNodeName());
            assertEquals(3, cluster.getNodes().stream().map(node -> node.getHttpAddress().getPort()).distinct().count());
            LocalTestCluster.Node dataNode = cluster.getNodeByName("data_0");
            String nodeId = dataNode.esNode().getNodeEnvironment().nodeId();
            dataNode.restart();
            cluster.waitForCluster(ClusterHealthStatus.GREEN, TimeValue.timeValueSeconds(30), 3);
            assertEquals(nodeId, dataNode.esNode().getNodeEnvironment().nodeId());
            cluster.close();
            assertFalse(cluster.isStarted());
        }
        assertFalse(Files.exists(home));
    }

    public void testFailedStartupCleansUpOtherNodes() throws Exception {
        Path home;
        try (
            LocalTestCluster cluster = new LocalTestCluster(
                "shared-failed-start",
                List.of(
                    new LocalTestCluster.NodeSpec(List.of("cluster_manager", "data"), PLUGINS),
                    new LocalTestCluster.NodeSpec(List.of("data"), PLUGINS)
                ),
                number -> number == 0 ? SETTINGS : Settings.builder().put(SETTINGS).put("unknown.fixture.setting", true).build(),
                null
            )
        ) {
            home = Path.of(cluster.getSnapshotDirPath()).getParent();
            Exception failure = expectThrows(Exception.class, cluster::start);
            assertTrue(failure.toString(), failure.toString().contains("unknown.fixture.setting"));
            assertFalse(cluster.isStarted());
            assertTrue(cluster.getNodes().isEmpty());
        }
        assertFalse(Files.exists(home));
    }
}
