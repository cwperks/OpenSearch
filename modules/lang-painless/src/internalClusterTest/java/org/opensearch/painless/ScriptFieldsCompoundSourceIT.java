/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.painless;

import org.opensearch.bootstrap.AgentAttach;
import org.opensearch.common.settings.Settings;
import org.opensearch.javaagent.bootstrap.AgentPolicy;
import org.opensearch.plugins.Plugin;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.lang.management.ManagementFactory;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertSearchResponse;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 1)
public class ScriptFieldsCompoundSourceIT extends OpenSearchIntegTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return List.of(PainlessModulePlugin.class);
    }

    public void testScriptFieldReadsSourceFromCompoundSegment() throws Exception {
        assertTrue("OpenSearch Java agent must be attached", AgentAttach.agentIsAttached());
        assertNotNull("Java agent policy must be installed", AgentPolicy.getPolicy());
        logger.info("Reproduction JVM arguments: {}", ManagementFactory.getRuntimeMXBean().getInputArguments());

        assertAcked(
            prepareCreate("compound-source").setSettings(
                Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 0)
                    .put("index.store.type", "mmapfs")
                    .put("index.use_compound_file", true)
            ).setMapping("{\"dynamic\":false}")
        );
        // Incompressible stored source large enough to exercise mmap read advice.
        byte[] padding = new byte[256 * 1024];
        new Random(0).nextBytes(padding);
        client().prepareIndex("compound-source")
            .setId("1")
            .setSource(Map.of("id_product", 42, "padding", Base64.getEncoder().encodeToString(padding)))
            .get();
        refresh("compound-source");

        var segments = client().admin()
            .indices()
            .prepareSegments("compound-source")
            .get()
            .getIndices()
            .get("compound-source")
            .getShards()
            .get(0)
            .getShards();
        assertEquals(1, segments.length);
        assertEquals(1, segments[0].getSegments().size());
        assertTrue("Reproduction requires a compound segment", segments[0].getSegments().get(0).isCompound());

        var ordinary = client().prepareSearch("compound-source").setSize(1).get();
        assertSearchResponse(ordinary);
        assertEquals(42, ordinary.getHits().getAt(0).getSourceAsMap().get("id_product"));

        var scripted = client().prepareSearch("compound-source")
            .setSize(1)
            .setFetchSource(false)
            .addScriptField("product", new Script(ScriptType.INLINE, "painless", "return params._source.id_product;", Map.of()))
            .get();
        assertSearchResponse(scripted);
        assertEquals(Integer.valueOf(42), scripted.getHits().getAt(0).field("product").getValue());
    }
}
