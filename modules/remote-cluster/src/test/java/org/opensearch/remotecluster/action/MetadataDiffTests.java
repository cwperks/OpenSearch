/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.action;

import org.opensearch.Version;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.IndexTemplateMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.common.bytes.BytesArray;
import org.opensearch.core.xcontent.MediaTypeRegistry;
import org.opensearch.remotecluster.provider.IndexMetadataDiffProvider;
import org.opensearch.remotecluster.provider.IngestPipelineDiffProvider;
import org.opensearch.remotecluster.provider.LegacyTemplateDiffProvider;
import org.opensearch.remotecluster.spi.CategoryDiffResult;
import org.opensearch.ingest.IngestMetadata;
import org.opensearch.ingest.PipelineConfiguration;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataDiffTests extends OpenSearchTestCase {

    public void testTemplatesInSync() {
        IndexTemplateMetadata template = IndexTemplateMetadata.builder("my-template").patterns(List.of("test-*")).build();
        Metadata local = Metadata.builder().put(template).build();
        Metadata remote = Metadata.builder().put(template).build();

        CategoryDiffResult result = new LegacyTemplateDiffProvider().diff(local, remote);
        assertEquals("compared", result.getStatus());
        assertEquals(1, result.getInSync());
        assertTrue(result.getRemoteOnly().isEmpty());
        assertTrue(result.getDiverged().isEmpty());
    }

    public void testTemplatesRemoteOnly() {
        IndexTemplateMetadata template = IndexTemplateMetadata.builder("remote-template").patterns(List.of("remote-*")).build();
        Metadata local = Metadata.builder().build();
        Metadata remote = Metadata.builder().put(template).build();

        CategoryDiffResult result = new LegacyTemplateDiffProvider().diff(local, remote);
        assertEquals(1, result.getRemoteOnly().size());
        assertEquals("remote-template", result.getRemoteOnly().get(0));
    }

    public void testIndicesRemoteOnly() {
        Metadata local = Metadata.builder().build();
        IndexMetadata indexMetadata = IndexMetadata.builder("test-index")
            .settings(
                Settings.builder()
                    .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
                    .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1)
            )
            .build();
        Metadata remote = Metadata.builder().put(indexMetadata, false).build();

        CategoryDiffResult result = new IndexMetadataDiffProvider().diff(local, remote);
        assertEquals(1, result.getRemoteOnly().size());
        assertEquals("test-index", result.getRemoteOnly().get(0));
    }

    public void testIndicesDivergedReplicas() {
        Settings baseSettings = Settings.builder()
            .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .build();
        IndexMetadata localIndex = IndexMetadata.builder("test-index")
            .settings(Settings.builder().put(baseSettings).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1))
            .build();
        IndexMetadata remoteIndex = IndexMetadata.builder("test-index")
            .settings(Settings.builder().put(baseSettings).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 2))
            .build();
        Metadata local = Metadata.builder().put(localIndex, false).build();
        Metadata remote = Metadata.builder().put(remoteIndex, false).build();

        CategoryDiffResult result = new IndexMetadataDiffProvider().diff(local, remote);
        assertEquals(0, result.getInSync());
        assertEquals(1, result.getDiverged().size());
        assertEquals("test-index", result.getDiverged().get(0).getName());
        assertTrue(
            result.getDiverged()
                .get(0)
                .getFields()
                .stream()
                .anyMatch(f -> f.getPath().contains("number_of_replicas") && "conditional".equals(f.getPolicy()))
        );
    }

    public void testSystemIndicesExcluded() {
        IndexMetadata systemIndex = IndexMetadata.builder(".system-index")
            .settings(
                Settings.builder()
                    .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
                    .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1)
            )
            .system(true)
            .build();
        Metadata local = Metadata.builder().build();
        Metadata remote = Metadata.builder().put(systemIndex, false).build();

        CategoryDiffResult result = new IndexMetadataDiffProvider().diff(local, remote);
        assertTrue(result.getRemoteOnly().isEmpty());
    }

    public void testIngestPipelinesRemoteOnly() {
        Map<String, PipelineConfiguration> pipelines = new HashMap<>();
        pipelines.put("my-pipeline", new PipelineConfiguration("my-pipeline", new BytesArray("{\"processors\":[]}"), MediaTypeRegistry.JSON));
        IngestMetadata ingestMetadata = new IngestMetadata(pipelines);

        Metadata local = Metadata.builder().build();
        Metadata remote = Metadata.builder().putCustom(IngestMetadata.TYPE, ingestMetadata).build();

        CategoryDiffResult result = new IngestPipelineDiffProvider().diff(local, remote);
        assertEquals(1, result.getRemoteOnly().size());
        assertEquals("my-pipeline", result.getRemoteOnly().get(0));
    }
}
