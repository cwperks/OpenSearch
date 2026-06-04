/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.provider;

import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.remotecluster.spi.CategoryDiffResult;
import org.opensearch.remotecluster.spi.DiffField;
import org.opensearch.remotecluster.spi.DivergedItem;
import org.opensearch.remotecluster.spi.MetadataDiffProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares index metadata between local and remote clusters.
 */
public class IndexMetadataDiffProvider implements MetadataDiffProvider {

    private static final Set<String> STRIPPED_SETTINGS = Set.of(
        "index.uuid",
        "index.version.created",
        "index.version.upgraded",
        "index.creation_date",
        "index.provided_name",
        "index.resize.source.uuid",
        "index.resize.source.name"
    );
    private static final List<String> STRIPPED_PREFIXES = List.of("index.routing.allocation.");
    private static final Set<String> CONDITIONAL_SETTINGS = Set.of("index.number_of_replicas");

    @Override
    public String category() {
        return "indices";
    }

    @Override
    public CategoryDiffResult diff(Metadata local, Metadata remote) {
        Set<String> localIndices = local.indices()
            .keySet()
            .stream()
            .filter(name -> isReplicable(local.index(name)))
            .collect(Collectors.toSet());
        Set<String> remoteIndices = remote.indices()
            .keySet()
            .stream()
            .filter(name -> isReplicable(remote.index(name)))
            .collect(Collectors.toSet());

        Set<String> remoteOnlySet = new HashSet<>(remoteIndices);
        remoteOnlySet.removeAll(localIndices);
        Set<String> localOnlySet = new HashSet<>(localIndices);
        localOnlySet.removeAll(remoteIndices);
        Set<String> common = new HashSet<>(localIndices);
        common.retainAll(remoteIndices);

        int inSync = 0;
        List<DivergedItem> diverged = new ArrayList<>();
        for (String name : common.stream().sorted().toList()) {
            List<DiffField> fields = diffIndex(local.index(name), remote.index(name));
            if (fields.isEmpty()) {
                inSync++;
            } else {
                diverged.add(new DivergedItem(name, fields));
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

    private List<DiffField> diffIndex(IndexMetadata local, IndexMetadata remote) {
        List<DiffField> fields = new ArrayList<>();

        String localMapping = local.mapping() != null ? local.mapping().source().string() : null;
        String remoteMapping = remote.mapping() != null ? remote.mapping().source().string() : null;
        if (localMapping != null ? !localMapping.equals(remoteMapping) : remoteMapping != null) {
            fields.add(new DiffField("mappings", "[differs]", "[differs]", "included"));
        }

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(local.getSettings().keySet());
        allKeys.addAll(remote.getSettings().keySet());

        for (String key : allKeys.stream().filter(k -> !isStripped(k)).sorted().toList()) {
            String localVal = local.getSettings().get(key);
            String remoteVal = remote.getSettings().get(key);
            if (localVal != null ? !localVal.equals(remoteVal) : remoteVal != null) {
                fields.add(new DiffField("settings." + key, localVal, remoteVal, policyFor(key)));
            }
        }

        Set<String> localAliases = local.getAliases().keySet();
        Set<String> remoteAliases = remote.getAliases().keySet();
        Set<String> aliasRemoteOnly = new HashSet<>(remoteAliases);
        aliasRemoteOnly.removeAll(localAliases);
        for (String alias : aliasRemoteOnly.stream().sorted().toList()) {
            fields.add(new DiffField("aliases." + alias, null, "present", "included"));
        }
        Set<String> aliasLocalOnly = new HashSet<>(localAliases);
        aliasLocalOnly.removeAll(remoteAliases);
        for (String alias : aliasLocalOnly.stream().sorted().toList()) {
            fields.add(new DiffField("aliases." + alias, "present", null, "included"));
        }

        return fields;
    }

    private boolean isReplicable(IndexMetadata im) {
        String name = im.getIndex().getName();
        return !name.startsWith(".") && !im.isSystem() && !im.getSettings().getAsBoolean("index.hidden", false);
    }

    private boolean isStripped(String key) {
        if (STRIPPED_SETTINGS.contains(key)) return true;
        return STRIPPED_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private String policyFor(String key) {
        return CONDITIONAL_SETTINGS.contains(key) ? "conditional" : "included";
    }
}
