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
import org.opensearch.core.common.Strings;
import org.opensearch.core.xcontent.MediaTypeRegistry;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;

public class ListMappingsResponseTests extends OpenSearchTestCase {

    public void testResponseUsesPaginatedMappingsEnvelope() {
        final MappingMetadata mapping = new MappingMetadata("_doc", Map.of("properties", Map.of("timestamp", Map.of("type", "date"))));
        final ListMappingsResponse response = new ListMappingsResponse(
            Map.of("logs-000001", mapping),
            new PageToken("next-page", MappingPagination.PAGINATED_ENTITY)
        );

        final String json = Strings.toString(MediaTypeRegistry.JSON, response);

        assertThat(json, containsString("\"next_token\":\"next-page\""));
        assertThat(json, containsString("\"mappings\""));
        assertThat(json, containsString("\"logs-000001\""));
        assertThat(json, containsString("\"timestamp\""));
    }
}
