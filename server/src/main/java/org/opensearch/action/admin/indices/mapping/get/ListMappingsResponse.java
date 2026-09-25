/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.admin.indices.mapping.get;

import org.opensearch.action.pagination.PageToken;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * Transport response for a paginated index mappings list request.
 *
 * @opensearch.internal
 */
public class ListMappingsResponse extends ActionResponse implements ToXContentObject {

    private final GetMappingsResponse mappings;
    private final PageToken pageToken;

    public ListMappingsResponse(Map<String, MappingMetadata> mappings, PageToken pageToken) {
        this(new GetMappingsResponse(mappings), pageToken);
    }

    public ListMappingsResponse(GetMappingsResponse mappings, PageToken pageToken) {
        this.mappings = mappings;
        this.pageToken = pageToken;
    }

    public ListMappingsResponse(StreamInput in) throws IOException {
        mappings = new GetMappingsResponse(in);
        pageToken = new PageToken(in);
    }

    public Map<String, MappingMetadata> mappings() {
        return mappings.mappings();
    }

    public PageToken pageToken() {
        return pageToken;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        mappings.writeTo(out);
        pageToken.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(PageToken.PAGINATED_RESPONSE_NEXT_TOKEN_KEY, pageToken.getNextToken());
        builder.startObject(pageToken.getPaginatedEntity());
        mappings.toXContent(builder, params);
        builder.endObject();
        builder.endObject();
        return builder;
    }
}
