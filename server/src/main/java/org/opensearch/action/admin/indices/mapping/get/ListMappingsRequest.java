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

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.pagination.PageParams;
import org.opensearch.action.support.clustermanager.info.ClusterInfoRequest;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;

import static org.opensearch.action.ValidateActions.addValidationError;

/**
 * Transport request to list index mappings in pages.
 *
 * @opensearch.internal
 */
public class ListMappingsRequest extends ClusterInfoRequest<ListMappingsRequest> {

    private PageParams pageParams;

    public ListMappingsRequest() {}

    public ListMappingsRequest(StreamInput in) throws IOException {
        super(in);
        pageParams = new PageParams(in);
    }

    public ListMappingsRequest pageParams(PageParams pageParams) {
        this.pageParams = pageParams;
        return this;
    }

    public PageParams pageParams() {
        return pageParams;
    }

    @Override
    public ActionRequestValidationException validate() {
        if (pageParams == null) {
            return addValidationError("page parameters are required", null);
        }
        return null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        pageParams.writeTo(out);
    }
}
