/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.cluster.snapshots.metadata;

import org.opensearch.cluster.metadata.ComponentTemplateMetadata;
import org.opensearch.cluster.metadata.ComposableIndexTemplateMetadata;
import org.opensearch.cluster.metadata.DataStreamMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.RepositoriesMetadata;
import org.opensearch.common.annotation.ExperimentalApi;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.ingest.IngestMetadata;
import org.opensearch.script.ScriptMetadata;
import org.opensearch.search.pipeline.SearchPipelineMetadata;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Names and setting keys only; never serializes metadata values or definitions. */
@ExperimentalApi
public class GetSnapshotMetadataResponse extends ActionResponse implements ToXContentObject {
    private final String repository;
    private final String snapshot;
    private final Boolean includeGlobalState;
    private final Map<String, List<String>> inventory;

    public GetSnapshotMetadataResponse(String repository, String snapshot, Boolean includeGlobalState, Metadata metadata) {
        this.repository = repository;
        this.snapshot = snapshot;
        this.includeGlobalState = includeGlobalState;
        Map<String, List<String>> names = new TreeMap<>();
        if (metadata != null) {
            names.put("persistent_setting_keys", sorted(metadata.persistentSettings().keySet()));
            names.put("legacy_index_templates", sorted(metadata.templates().keySet()));
            names.put("index_templates", sorted(metadata.templatesV2().keySet()));
            names.put("component_templates", sorted(metadata.componentTemplates().keySet()));
            IngestMetadata ingest = metadata.custom(IngestMetadata.TYPE);
            names.put("ingest_pipelines", ingest == null ? List.of() : sorted(ingest.getPipelines().keySet()));
            SearchPipelineMetadata search = metadata.custom(SearchPipelineMetadata.TYPE);
            names.put("search_pipelines", search == null ? List.of() : sorted(search.getPipelines().keySet()));
            ScriptMetadata scripts = metadata.custom(ScriptMetadata.TYPE);
            names.put("stored_scripts", scripts == null ? List.of() : sorted(scripts.getStoredScriptIds()));
            Set<String> knownTypes = Set.of(
                ComponentTemplateMetadata.TYPE,
                ComposableIndexTemplateMetadata.TYPE,
                IngestMetadata.TYPE,
                SearchPipelineMetadata.TYPE,
                ScriptMetadata.TYPE,
                RepositoriesMetadata.TYPE,
                DataStreamMetadata.TYPE
            );
            names.put(
                "other_custom_metadata_types",
                metadata.customs()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().context().contains(Metadata.XContentContext.SNAPSHOT))
                    .map(Map.Entry::getKey)
                    .filter(type -> knownTypes.contains(type) == false)
                    .sorted()
                    .toList()
            );
        }
        inventory = Collections.unmodifiableMap(names);
    }

    public GetSnapshotMetadataResponse(StreamInput in) throws IOException {
        super(in);
        repository = in.readString();
        snapshot = in.readString();
        includeGlobalState = in.readOptionalBoolean();
        inventory = Collections.unmodifiableMap(
            new TreeMap<>(in.readMap(StreamInput::readString, input -> List.copyOf(input.readStringList())))
        );
    }

    private static List<String> sorted(Set<String> names) {
        return names.stream().sorted().toList();
    }

    public Map<String, List<String>> inventory() {
        return inventory;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(repository);
        out.writeString(snapshot);
        out.writeOptionalBoolean(includeGlobalState);
        out.writeMap(inventory, StreamOutput::writeString, StreamOutput::writeStringCollection);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("repository", repository);
        builder.field("snapshot", snapshot);
        builder.field("include_global_state", includeGlobalState);
        // Null also covers old snapshots whose inclusion flag is unknown.
        builder.field("global_metadata", inventory.isEmpty() ? null : inventory);
        return builder.endObject();
    }
}
