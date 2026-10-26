/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.test;

import org.opensearch.common.settings.Settings;
import org.opensearch.core.common.transport.TransportAddress;
import org.opensearch.env.Environment;
import org.opensearch.http.HttpServerTransport;
import org.opensearch.node.InternalSettingsPreparer;
import org.opensearch.node.Node;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.transport.TransportService;
import org.opensearch.transport.client.Client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A composable, single-node cluster using production node services and classpath plugins.
 * Unlike {@link OpenSearchIntegTestCase}, this fixture does not require inheritance or a
 * randomized runner. Callers own the home directory, HTTP client, and plugin-specific setup.
 * Plugin descriptors retain extension relationships; this is not a packaged-plugin test.
 *
 * @opensearch.internal
 */
public final class LocalTestCluster implements AutoCloseable {
    private final Path home;
    private final Settings settings;
    private final List<PluginInfo> plugins;
    private Node node;

    /** Creates a stopped fixture. The caller must use a dedicated home directory. */
    public LocalTestCluster(Path home, Settings settings, Collection<PluginInfo> plugins) {
        this.home = Objects.requireNonNull(home);
        this.settings = Objects.requireNonNull(settings);
        this.plugins = List.copyOf(plugins);
    }

    /** Starts the node, closing it if startup fails. The OS selects unused listening ports. */
    public synchronized void start() throws Exception {
        if (node != null) {
            throw new IllegalStateException("Cluster is already started");
        }
        Settings nodeSettings = Settings.builder()
            .put("cluster.name", "local-test-cluster")
            .put("node.name", "local-test-node")
            .put("network.host", "127.0.0.1")
            .put("cluster.routing.allocation.disk.threshold_enabled", false)
            .put(settings)
            .put("path.home", home)
            .put("discovery.type", "single-node")
            .put("http.port", 0)
            .put("transport.port", 0)
            .build();
        Environment environment = InternalSettingsPreparer.prepareEnvironment(nodeSettings, Map.of(), null, () -> "localhost");
        node = new ClasspathNode(environment, plugins);
        try {
            node.start();
        } catch (Exception | Error failure) {
            try {
                close();
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    /** Returns the node's bound HTTP address; the caller chooses HTTP or HTTPS. */
    public synchronized TransportAddress httpAddress() {
        return node().injector().getInstance(HttpServerTransport.class).boundAddress().publishAddress();
    }

    /** Returns the node's bound transport address. */
    public synchronized TransportAddress transportAddress() {
        return node().injector().getInstance(TransportService.class).boundAddress().publishAddress();
    }

    /** Returns the internal client, which does not represent an authenticated HTTP user. */
    public synchronized Client client() {
        return node().client();
    }

    private Node node() {
        if (node == null) {
            throw new IllegalStateException("Cluster is not started");
        }
        return node;
    }

    /** Closes and awaits the node. The caller remains responsible for its temporary directory. */
    @Override
    public synchronized void close() throws IOException {
        if (node == null) {
            return;
        }
        node.close();
        try {
            if (node.awaitClose(30, TimeUnit.SECONDS) == false) {
                throw new IOException("Timed out closing local test cluster");
            }
            node = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted closing local test cluster", e);
        }
    }

    private static final class ClasspathNode extends Node {
        private ClasspathNode(Environment environment, Collection<PluginInfo> plugins) {
            super(environment, plugins, true);
        }
    }
}
