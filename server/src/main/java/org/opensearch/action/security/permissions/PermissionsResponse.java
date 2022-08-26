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

import org.apache.lucene.search.Explanation;
import org.opensearch.Version;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.explain.ExplainResponse;
import org.opensearch.common.ParseField;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ConstructingObjectParser;
import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.index.get.GetResult;
import org.opensearch.index.mapper.MapperService;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.opensearch.common.lucene.Lucene.readExplanation;
import static org.opensearch.common.lucene.Lucene.writeExplanation;

/**
 * Response containing the score explanation.
 *
 * @opensearch.internal
 */
public class PermissionsResponse extends ActionResponse implements StatusToXContentObject {

    private static final ParseField _USER_ID = new ParseField("_user_id");

    private static final ParseField _PERMISSIONS = new ParseField("_permissions");

    private static final ParseField _ROLES = new ParseField("_roles");

    private String userId;

    private List<String> permissions;

    private Map<String, List<String>> roles;

    private boolean exists;
    public PermissionsResponse(String userId, List<String> permissions, Map<String, List<String>> roles, boolean exists) {
        this.userId = userId;
        this.permissions = permissions;
        this.roles = roles;
        this.exists = exists;
    }

    public PermissionsResponse(StreamInput in) throws IOException {
        super(in);
        userId = in.readString();
        permissions = in.readStringList();
        roles = in.readMapOfLists(StreamInput::readString, StreamInput::readString);
        if (in.getVersion().before(Version.V_2_0_0)) {
            in.readString();
        }
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(userId);
        // TODO Write Permissions
        out.writeCollection(permissions, StreamOutput::writeString);
        out.writeMapOfLists(roles, StreamOutput::writeString, StreamOutput::writeString);
        if (out.getVersion().before(Version.V_2_0_0)) {
            out.writeString(MapperService.SINGLE_MAPPING_NAME);
        }
        out.writeBoolean(exists);
    }

    private static final ConstructingObjectParser<PermissionsResponse, Boolean> PARSER = new ConstructingObjectParser<>(
        "permissions",
        true,
        (arg, exists) -> new PermissionsResponse((String) arg[0], (List<String>) arg[1], (Map<String, List<String>>) arg[2], exists)
    );

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), _USER_ID);
    }

    public static PermissionsResponse fromXContent(XContentParser parser, boolean exists) {
        return PARSER.apply(parser, exists);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(_USER_ID.getPreferredName(), userId);
        builder.field(_PERMISSIONS.getPreferredName(), permissions);
        builder.field(_ROLES.getPreferredName(), roles);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PermissionsResponse other = (PermissionsResponse) obj;
        return userId.equals(other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public RestStatus status() {
        return exists ? RestStatus.OK : RestStatus.NOT_FOUND;
    }
}
