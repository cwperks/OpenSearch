/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.provider;

import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.remotecluster.spi.CategoryDiffResult;
import org.opensearch.remotecluster.spi.DiffField;
import org.opensearch.remotecluster.spi.DivergedItem;
import org.opensearch.remotecluster.spi.MetadataDiffProvider;
import org.opensearch.ingest.IngestMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares ingest pipelines between local and remote clusters.
 */
public class IngestPipelineDiffProvider implements MetadataDiffProvider {
    @Override
    public String category() {
        return "ingest_pipelines";
    }

    @Override
    public CategoryDiffResult diff(Metadata local, Metadata remote) {
        IngestMetadata localIngest = local.custom(IngestMetadata.TYPE);
        IngestMetadata remoteIngest = remote.custom(IngestMetadata.TYPE);

        Set<String> localNames = localIngest != null ? localIngest.getPipelines().keySet() : Collections.emptySet();
        Set<String> remoteNames = remoteIngest != null ? remoteIngest.getPipelines().keySet() : Collections.emptySet();

        Set<String> remoteOnlySet = new HashSet<>(remoteNames);
        remoteOnlySet.removeAll(localNames);
        Set<String> localOnlySet = new HashSet<>(localNames);
        localOnlySet.removeAll(remoteNames);
        Set<String> common = new HashSet<>(localNames);
        common.retainAll(remoteNames);

        int inSync = 0;
        List<DivergedItem> diverged = new ArrayList<>();
        for (String name : common) {
            if (localIngest.getPipelines().get(name).getConfigAsMap().equals(remoteIngest.getPipelines().get(name).getConfigAsMap())) {
                inSync++;
            } else {
                diverged.add(new DivergedItem(name, List.of(new DiffField("content", "[differs]", "[differs]", "included"))));
            }
        }

        return new CategoryDiffResult(
            category(),
            "compared",
            inSync,
            remoteOnlySet.stream().sorted().toList(),
            localOnlySet.stream().sorted().toList(),
            diverged
        );
    }
}
