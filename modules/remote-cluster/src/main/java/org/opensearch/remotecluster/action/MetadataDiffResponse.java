/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.action;

import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.remotecluster.spi.CategoryDiffResult;

import java.io.IOException;
import java.util.List;

/**
 * Response containing metadata diff results.
 */
public class MetadataDiffResponse extends ActionResponse implements ToXContentObject {
    private final String connectionName;
    private final long remoteMetadataVersion;
    private final long localMetadataVersion;
    private final List<CategoryDiffResult> categories;

    public MetadataDiffResponse(
        String connectionName,
        long remoteMetadataVersion,
        long localMetadataVersion,
        List<CategoryDiffResult> categories
    ) {
        this.connectionName = connectionName;
        this.remoteMetadataVersion = remoteMetadataVersion;
        this.localMetadataVersion = localMetadataVersion;
        this.categories = categories;
    }

    public MetadataDiffResponse(StreamInput in) throws IOException {
        super(in);
        this.connectionName = in.readString();
        this.remoteMetadataVersion = in.readVLong();
        this.localMetadataVersion = in.readVLong();
        this.categories = List.of(); // TODO: full serialization
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(connectionName);
        out.writeVLong(remoteMetadataVersion);
        out.writeVLong(localMetadataVersion);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("connection_name", connectionName);
        builder.field("remote_metadata_version", remoteMetadataVersion);
        builder.field("local_metadata_version", localMetadataVersion);
        builder.startObject("categories");
        for (CategoryDiffResult cat : categories) {
            builder.field(cat.getCategory());
            cat.toXContent(builder, params);
        }
        builder.endObject();
        builder.endObject();
        return builder;
    }
}
