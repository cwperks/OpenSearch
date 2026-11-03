/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.snapshots;

import org.opensearch.action.admin.cluster.snapshots.metadata.GetSnapshotMetadataAction;
import org.opensearch.action.admin.cluster.snapshots.metadata.GetSnapshotMetadataRequest;
import org.opensearch.action.admin.cluster.snapshots.metadata.GetSnapshotMetadataResponse;
import org.opensearch.repositories.RepositoryMissingException;

import java.util.List;

import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

public class SnapshotMetadataIT extends AbstractSnapshotIntegTestCase {
    public void testInspectsSnapshotRatherThanLiveMetadata() {
        createRepository("metadata-repo", "fs");
        assertAcked(client().admin().indices().preparePutTemplate("snapshot-template").setPatterns(List.of("test-*")).get());
        createFullSnapshot("metadata-repo", "with-state");
        assertAcked(client().admin().indices().prepareDeleteTemplate("snapshot-template").get());
        assertAcked(client().admin().indices().preparePutTemplate("live-template").setPatterns(List.of("live-*")).get());

        GetSnapshotMetadataResponse response = client().execute(
            GetSnapshotMetadataAction.INSTANCE,
            new GetSnapshotMetadataRequest("metadata-repo", "with-state")
        ).actionGet();
        assertTrue(response.inventory().get("legacy_index_templates").contains("snapshot-template"));
        assertFalse(response.inventory().get("legacy_index_templates").contains("live-template"));
        assertTrue(client().admin().indices().prepareGetTemplates("snapshot-template").get().getIndexTemplates().isEmpty());
        assertEquals(1, client().admin().indices().prepareGetTemplates("live-template").get().getIndexTemplates().size());

        client().admin()
            .cluster()
            .prepareCreateSnapshot("metadata-repo", "without-state")
            .setIncludeGlobalState(false)
            .setWaitForCompletion(true)
            .get();
        assertTrue(
            client().execute(GetSnapshotMetadataAction.INSTANCE, new GetSnapshotMetadataRequest("metadata-repo", "without-state"))
                .actionGet()
                .inventory()
                .isEmpty()
        );

        expectThrows(
            SnapshotMissingException.class,
            () -> client().execute(GetSnapshotMetadataAction.INSTANCE, new GetSnapshotMetadataRequest("metadata-repo", "missing"))
                .actionGet()
        );
        expectThrows(
            RepositoryMissingException.class,
            () -> client().execute(GetSnapshotMetadataAction.INSTANCE, new GetSnapshotMetadataRequest("missing", "snapshot")).actionGet()
        );
    }
}
