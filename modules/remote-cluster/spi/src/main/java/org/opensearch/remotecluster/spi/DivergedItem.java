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
 * An item that has diverged between local and remote clusters.
 */
public class DivergedItem implements ToXContentObject {
    private final String name;
    private final List<DiffField> fields;

    public DivergedItem(String name, List<DiffField> fields) {
        this.name = name;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public List<DiffField> getFields() {
        return fields;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("name", name);
        builder.startArray("fields");
        for (DiffField field : fields) {
            field.toXContent(builder, params);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }
}
