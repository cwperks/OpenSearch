/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.security.permissions;

import org.opensearch.Version;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.ValidateActions;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import org.opensearch.action.support.single.shard.SingleShardRequest;
import org.opensearch.common.ParseField;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.index.mapper.MapperService;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;
import org.opensearch.search.internal.AliasFilter;

import java.io.IOException;

import static org.opensearch.action.ValidateActions.addValidationError;

/**
 * Explain request encapsulating the explain query and document identifier to get an explanation for.
 *
 * @opensearch.internal
 */
public class PermissionsRequest extends ClusterManagerNodeRequest<PermissionsRequest> implements ToXContentObject {

    private static final ParseField QUERY_FIELD = new ParseField("query");

    private String userId;

    long nowInMillis;

    public PermissionsRequest() {}

    public PermissionsRequest(String userId) {
        this.userId = userId;
    }

    PermissionsRequest(StreamInput in) throws IOException {
        super(in);
        if (in.getVersion().before(Version.V_2_0_0)) {
            in.readString();
        }
        userId = in.readString();
        nowInMillis = in.readVLong();
    }

    public String userId() {
        return userId;
    }

    public PermissionsRequest userId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("_user_id", userId);
        builder.endObject();
        return builder;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (Strings.isEmpty(userId)) {
            return addValidationError("userId is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        if (out.getVersion().before(Version.V_2_0_0)) {
            out.writeString(MapperService.SINGLE_MAPPING_NAME);
        }
        out.writeString(userId);
        out.writeVLong(nowInMillis);
    }
}
