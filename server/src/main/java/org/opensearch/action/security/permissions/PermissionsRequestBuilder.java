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

import org.opensearch.action.support.single.shard.SingleShardOperationRequestBuilder;
import org.opensearch.client.OpenSearchClient;
import org.opensearch.common.Nullable;
import org.opensearch.common.Strings;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

/**
 * A builder for {@link PermissionsRequest}.
 *
 * @opensearch.internal
 */
public class PermissionsRequestBuilder extends SingleShardOperationRequestBuilder<PermissionsRequest, PermissionsResponse, PermissionsRequestBuilder> {

    PermissionsRequestBuilder(OpenSearchClient client, PermissionsAction action) {
        super(client, action, new PermissionsRequest());
    }

    public PermissionsRequestBuilder(OpenSearchClient client, PermissionsAction action, String index, String id) {
        super(client, action, new PermissionsRequest().index(index).id(id));
    }

    /**
     * Sets the id to get a score explanation for.
     */
    public PermissionsRequestBuilder setId(String id) {
        request().id(id);
        return this;
    }

    /**
     * Sets the routing for sharding.
     */
    public PermissionsRequestBuilder setRouting(String routing) {
        request().routing(routing);
        return this;
    }

    /**
     * Simple sets the routing. Since the parent is only used to get to the right shard.
     */
    public PermissionsRequestBuilder setParent(String parent) {
        request().parent(parent);
        return this;
    }

    /**
     * Sets the shard preference.
     */
    public PermissionsRequestBuilder setPreference(String preference) {
        request().preference(preference);
        return this;
    }

    /**
     * Sets the query to get a score explanation for.
     */
    public PermissionsRequestBuilder setQuery(QueryBuilder query) {
        request.query(query);
        return this;
    }

    /**
     * Explicitly specify the stored fields that will be returned for the explained document. By default, nothing is returned.
     */
    public PermissionsRequestBuilder setStoredFields(String... fields) {
        request.storedFields(fields);
        return this;
    }

    /**
     * Indicates whether the response should contain the stored _source
     */
    public PermissionsRequestBuilder setFetchSource(boolean fetch) {
        FetchSourceContext fetchSourceContext = request.fetchSourceContext() != null
            ? request.fetchSourceContext()
            : FetchSourceContext.FETCH_SOURCE;
        request.fetchSourceContext(new FetchSourceContext(fetch, fetchSourceContext.includes(), fetchSourceContext.excludes()));
        return this;
    }

    /**
     * Indicate that _source should be returned, with an "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param include An optional include (optionally wildcarded) pattern to filter the returned _source
     * @param exclude An optional exclude (optionally wildcarded) pattern to filter the returned _source
     */
    public PermissionsRequestBuilder setFetchSource(@Nullable String include, @Nullable String exclude) {
        return setFetchSource(
            include == null ? Strings.EMPTY_ARRAY : new String[] { include },
            exclude == null ? Strings.EMPTY_ARRAY : new String[] { exclude }
        );
    }

    /**
     * Indicate that _source should be returned, with an "include" and/or "exclude" set which can include simple wildcard
     * elements.
     *
     * @param includes An optional list of include (optionally wildcarded) pattern to filter the returned _source
     * @param excludes An optional list of exclude (optionally wildcarded) pattern to filter the returned _source
     */
    public PermissionsRequestBuilder setFetchSource(@Nullable String[] includes, @Nullable String[] excludes) {
        FetchSourceContext fetchSourceContext = request.fetchSourceContext() != null
            ? request.fetchSourceContext()
            : FetchSourceContext.FETCH_SOURCE;
        request.fetchSourceContext(new FetchSourceContext(fetchSourceContext.fetchSource(), includes, excludes));
        return this;
    }
}
