/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache 2.0 license or a
 * compatible open source license.
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
