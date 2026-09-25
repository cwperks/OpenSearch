/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.spi;

import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Result of comparing metadata for a single category.
 */
public class CategoryDiffResult implements ToXContentObject {
    private final String category;
    private final String status;
    private final int inSync;
    private final List<String> remoteOnly;
    private final List<String> localOnly;
    private final List<DivergedItem> diverged;

    public CategoryDiffResult(
        String category,
        String status,
        int inSync,
        List<String> remoteOnly,
        List<String> localOnly,
        List<DivergedItem> diverged
    ) {
        this.category = category;
        this.status = status;
        this.inSync = inSync;
        this.remoteOnly = remoteOnly;
        this.localOnly = localOnly;
        this.diverged = diverged;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public int getInSync() {
        return inSync;
    }

    public List<String> getRemoteOnly() {
        return remoteOnly;
    }

    public List<String> getLocalOnly() {
        return localOnly;
    }

    public List<DivergedItem> getDiverged() {
        return diverged;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("status", status);
        builder.field("in_sync", inSync);
        builder.field("remote_only", remoteOnly.size());
        builder.field("local_only", localOnly.size());
        builder.field("diverged", diverged.size());
        if (!remoteOnly.isEmpty() || !localOnly.isEmpty() || !diverged.isEmpty()) {
            builder.startObject("items");
            if (!remoteOnly.isEmpty()) {
                builder.field("remote_only", remoteOnly);
            }
            if (!localOnly.isEmpty()) {
                builder.field("local_only", localOnly);
            }
            if (!diverged.isEmpty()) {
                builder.startArray("diverged");
                for (DivergedItem item : diverged) {
                    item.toXContent(builder, params);
                }
                builder.endArray();
            }
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }
}
