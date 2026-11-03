/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.cluster.snapshots.metadata;

import org.opensearch.cluster.metadata.ComponentTemplate;
import org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.opensearch.cluster.metadata.ComposableIndexTemplateMetadata;
import org.opensearch.cluster.metadata.IndexTemplateMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.Template;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.bytes.BytesArray;
import org.opensearch.core.xcontent.MediaTypeRegistry;
import org.opensearch.ingest.IngestMetadata;
import org.opensearch.ingest.PipelineConfiguration;
import org.opensearch.persistent.PersistentTasksCustomMetadata;
import org.opensearch.script.ScriptMetadata;
import org.opensearch.script.StoredScriptSource;
import org.opensearch.search.pipeline.SearchPipelineMetadata;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;
import java.util.Map;

public class GetSnapshotMetadataTests extends OpenSearchTestCase {
    public void testNamesOnlyAndWireRoundTrip() throws Exception {
        Metadata metadata = Metadata.builder()
            .persistentSettings(Settings.builder().put("cluster.test.secret", "sensitive-setting-value").build())
            .put(IndexTemplateMetadata.builder("z-template").patterns(List.of("private-*")).build())
            .put(IndexTemplateMetadata.builder("a-template").patterns(List.of("private-*")).build())
            .putCustom(
                ScriptMetadata.TYPE,
                new ScriptMetadata.Builder(null).storeScript(
                    "test-script",
                    new StoredScriptSource("painless", "sensitive-script-body", Map.of())
                ).build()
            )
            .build();
        GetSnapshotMetadataResponse response = new GetSnapshotMetadataResponse("repo", "snap", true, metadata);
        assertEquals(List.of("a-template", "z-template"), response.inventory().get("legacy_index_templates"));
        assertEquals(List.of("test-script"), response.inventory().get("stored_scripts"));
        String json = Strings.toString(MediaTypeRegistry.JSON, response);
        assertTrue(json.contains("cluster.test.secret"));
        assertFalse(json.contains("sensitive-setting-value"));
        assertFalse(json.contains("sensitive-script-body"));
        assertFalse(json.contains("private-*"));
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            response.writeTo(out);
            GetSnapshotMetadataResponse copy = new GetSnapshotMetadataResponse(out.bytes().streamInput());
            assertEquals(json, Strings.toString(MediaTypeRegistry.JSON, copy));
        }
    }

    public void testAbsentAndEmptyGlobalMetadataDiffer() {
        GetSnapshotMetadataResponse absent = new GetSnapshotMetadataResponse("repo", "snap", false, null);
        assertTrue(Strings.toString(MediaTypeRegistry.JSON, absent).contains("\"global_metadata\":null"));
        GetSnapshotMetadataResponse empty = new GetSnapshotMetadataResponse("repo", "snap", true, Metadata.EMPTY_METADATA);
        assertEquals(List.of(), empty.inventory().get("persistent_setting_keys"));
        assertFalse(empty.inventory().isEmpty());
        assertEquals(List.of(), empty.inventory().get("other_custom_metadata_types"));
        GetSnapshotMetadataResponse unknown = new GetSnapshotMetadataResponse("repo", "old", null, null);
        assertTrue(Strings.toString(MediaTypeRegistry.JSON, unknown).contains("\"include_global_state\":null"));
    }

    public void testTemplatePipelineAndCustomMetadataNames() {
        Metadata metadata = Metadata.builder()
            .componentTemplates(Map.of("component", new ComponentTemplate(new Template(null, null, null), null, null)))
            .putCustom(
                ComposableIndexTemplateMetadata.TYPE,
                new ComposableIndexTemplateMetadata(
                    Map.of("composable", new ComposableIndexTemplate(List.of("logs-*"), null, List.of("component"), null, null, null))
                )
            )
            .putCustom(
                IngestMetadata.TYPE,
                new IngestMetadata(Map.of("ingest", new PipelineConfiguration("ingest", new BytesArray("{}"), MediaTypeRegistry.JSON)))
            )
            .putCustom(
                SearchPipelineMetadata.TYPE,
                new SearchPipelineMetadata(
                    Map.of(
                        "search",
                        new org.opensearch.search.pipeline.PipelineConfiguration("search", new BytesArray("{}"), MediaTypeRegistry.JSON)
                    )
                )
            )
            .putCustom(PersistentTasksCustomMetadata.TYPE, PersistentTasksCustomMetadata.builder().build())
            .build();
        Map<String, List<String>> inventory = new GetSnapshotMetadataResponse("repo", "snap", true, metadata).inventory();
        assertEquals(List.of("component"), inventory.get("component_templates"));
        assertEquals(List.of("composable"), inventory.get("index_templates"));
        assertEquals(List.of("ingest"), inventory.get("ingest_pipelines"));
        assertEquals(List.of("search"), inventory.get("search_pipelines"));
        assertEquals(List.of(PersistentTasksCustomMetadata.TYPE), inventory.get("other_custom_metadata_types"));
    }

    public void testRequestValidationAndWireRoundTrip() throws Exception {
        assertEquals(2, new GetSnapshotMetadataRequest(null, "").validate().validationErrors().size());
        GetSnapshotMetadataRequest request = new GetSnapshotMetadataRequest("repo", "snap");
        assertNull(request.validate());
        try (BytesStreamOutput out = new BytesStreamOutput()) {
            request.writeTo(out);
            GetSnapshotMetadataRequest copy = new GetSnapshotMetadataRequest(out.bytes().streamInput());
            assertEquals("repo", copy.repository());
            assertEquals("snap", copy.snapshot());
        }
    }
}
