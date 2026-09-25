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

import org.opensearch.OpenSearchParseException;
import org.opensearch.action.pagination.PageParams;
import org.opensearch.test.OpenSearchTestCase;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class MappingPaginationTests extends OpenSearchTestCase {

    public void testAscendingPaginationReturnsAllIndicesInOrder() {
        final String[] indices = new String[] { "logs-000003", "logs-000001", "logs-000002" };

        final MappingPagination.Page firstPage = MappingPagination.paginate(
            indices,
            new PageParams(null, PageParams.PARAM_ASC_SORT_VALUE, 2)
        );
        assertThat(firstPage.indices(), contains("logs-000001", "logs-000002"));
        assertThat(firstPage.pageToken().getNextToken(), not(nullValue()));

        final MappingPagination.Page secondPage = MappingPagination.paginate(
            indices,
            new PageParams(firstPage.pageToken().getNextToken(), PageParams.PARAM_ASC_SORT_VALUE, 2)
        );
        assertThat(secondPage.indices(), contains("logs-000003"));
        assertThat(secondPage.pageToken().getNextToken(), nullValue());
    }

    public void testDescendingPaginationReturnsAllIndicesInOrder() {
        final MappingPagination.Page firstPage = MappingPagination.paginate(
            new String[] { "logs-000001", "logs-000003", "logs-000002" },
            new PageParams(null, PageParams.PARAM_DESC_SORT_VALUE, 2)
        );
        assertThat(firstPage.indices(), contains("logs-000003", "logs-000002"));

        final MappingPagination.Page secondPage = MappingPagination.paginate(
            new String[] { "logs-000001", "logs-000003", "logs-000002" },
            new PageParams(firstPage.pageToken().getNextToken(), PageParams.PARAM_DESC_SORT_VALUE, 2)
        );
        assertThat(secondPage.indices(), contains("logs-000001"));
        assertThat(secondPage.pageToken().getNextToken(), nullValue());
    }

    public void testPaginationDoesNotDuplicateIndices() {
        final MappingPagination.Page page = MappingPagination.paginate(
            new String[] { "logs-000001", "logs-000001", "logs-000002" },
            new PageParams(null, PageParams.PARAM_ASC_SORT_VALUE, 10)
        );

        assertThat(page.indices(), contains("logs-000001", "logs-000002"));
    }

    public void testRejectsMalformedToken() {
        final OpenSearchParseException exception = assertThrows(
            OpenSearchParseException.class,
            () -> MappingPagination.paginate(
                new String[] { "logs-000001" },
                new PageParams("not-a-valid-token", PageParams.PARAM_ASC_SORT_VALUE, 1)
            )
        );

        assertThat(exception.getMessage(), containsString("next_token"));
    }

    public void testRejectsTokenWithDifferentSort() {
        final MappingPagination.Page firstPage = MappingPagination.paginate(
            new String[] { "logs-000001", "logs-000002" },
            new PageParams(null, PageParams.PARAM_ASC_SORT_VALUE, 1)
        );

        final OpenSearchParseException exception = assertThrows(
            OpenSearchParseException.class,
            () -> MappingPagination.paginate(
                new String[] { "logs-000001", "logs-000002" },
                new PageParams(firstPage.pageToken().getNextToken(), PageParams.PARAM_DESC_SORT_VALUE, 1)
            )
        );

        assertThat(exception.getMessage(), containsString("sort"));
    }

    public void testIndicesAddedBeforeCursorAreNotDuplicated() {
        final MappingPagination.Page firstPage = MappingPagination.paginate(
            new String[] { "logs-000002", "logs-000003" },
            new PageParams(null, PageParams.PARAM_ASC_SORT_VALUE, 1)
        );

        final MappingPagination.Page secondPage = MappingPagination.paginate(
            new String[] { "logs-000001", "logs-000002", "logs-000003" },
            new PageParams(firstPage.pageToken().getNextToken(), PageParams.PARAM_ASC_SORT_VALUE, 1)
        );

        assertThat(secondPage.indices(), contains("logs-000003"));
    }
}
