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

/**
 * A specific field that differs between local and remote metadata.
 */
public class DiffField implements ToXContentObject {
    private final String path;
    private final String local;
    private final String remote;
    private final String policy;

    public DiffField(String path, String local, String remote, String policy) {
        this.path = path;
        this.local = local;
        this.remote = remote;
        this.policy = policy;
    }

    public String getPath() {
        return path;
    }

    public String getLocal() {
        return local;
    }

    public String getRemote() {
        return remote;
    }

    public String getPolicy() {
        return policy;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("path", path);
        builder.field("local", local);
        builder.field("remote", remote);
        builder.field("policy", policy);
        builder.endObject();
        return builder;
    }
}
