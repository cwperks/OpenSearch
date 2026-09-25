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

package org.opensearch.rest.action.admin.indices;

import org.opensearch.action.admin.indices.mapping.get.ListMappingsAction;
import org.opensearch.action.admin.indices.mapping.get.ListMappingsRequest;
import org.opensearch.action.admin.indices.mapping.get.ListMappingsResponse;
import org.opensearch.action.admin.indices.mapping.get.MappingPagination;
import org.opensearch.action.pagination.PageParams;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.core.common.Strings;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.action.RestBuilderListener;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.action.pagination.PageParams.PARAM_ASC_SORT_VALUE;
import static org.opensearch.action.pagination.PageParams.PARAM_DESC_SORT_VALUE;
import static org.opensearch.rest.RestRequest.Method.GET;

/**
 * REST handler for paginated index mappings list requests.
 *
 * @opensearch.api
 */
public class RestListMappingsAction extends BaseRestHandler {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 1_000;

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_list/mappings"), new Route(GET, "/_list/mappings/{index}")));
    }

    @Override
    public String getName() {
        return "list_mappings_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        final ListMappingsRequest listMappingsRequest = new ListMappingsRequest();
        listMappingsRequest.indices(Strings.splitStringByCommaToArray(request.param("index")));
        listMappingsRequest.indicesOptions(IndicesOptions.fromRequest(request, listMappingsRequest.indicesOptions()));
        listMappingsRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", listMappingsRequest.clusterManagerNodeTimeout())
        );
        listMappingsRequest.local(request.paramAsBoolean("local", listMappingsRequest.local()));
        listMappingsRequest.pageParams(parsePageParams(request));

        return channel -> client.execute(
            ListMappingsAction.INSTANCE,
            listMappingsRequest,
            new RestBuilderListener<ListMappingsResponse>(channel) {
                @Override
                public RestResponse buildResponse(ListMappingsResponse response, XContentBuilder builder) throws Exception {
                    response.toXContent(builder, request);
                    return new BytesRestResponse(RestStatus.OK, builder);
                }
            }
        );
    }

    private static PageParams parsePageParams(RestRequest request) {
        final PageParams pageParams = request.parsePaginatedQueryParams(PARAM_ASC_SORT_VALUE, DEFAULT_PAGE_SIZE);
        if (pageParams.getSize() <= 0 || pageParams.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (Objects.equals(pageParams.getSort(), PARAM_ASC_SORT_VALUE) == false
            && Objects.equals(pageParams.getSort(), PARAM_DESC_SORT_VALUE) == false) {
            throw new IllegalArgumentException("value of sort can either be asc or desc");
        }
        MappingPagination.validateToken(pageParams.getRequestedToken(), pageParams.getSort());
        return pageParams;
    }
}
