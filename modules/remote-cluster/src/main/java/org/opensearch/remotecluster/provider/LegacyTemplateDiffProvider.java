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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares legacy index templates between local and remote clusters.
 */
public class LegacyTemplateDiffProvider implements MetadataDiffProvider {
    @Override
    public String category() {
        return "templates";
    }

    @Override
    public CategoryDiffResult diff(Metadata local, Metadata remote) {
        Set<String> localNames = local.templates().keySet();
        Set<String> remoteNames = remote.templates().keySet();

        Set<String> remoteOnlySet = new HashSet<>(remoteNames);
        remoteOnlySet.removeAll(localNames);
        Set<String> localOnlySet = new HashSet<>(localNames);
        localOnlySet.removeAll(remoteNames);
        Set<String> common = new HashSet<>(localNames);
        common.retainAll(remoteNames);

        int inSync = 0;
        List<DivergedItem> diverged = new ArrayList<>();
        for (String name : common) {
            if (local.templates().get(name).equals(remote.templates().get(name))) {
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
