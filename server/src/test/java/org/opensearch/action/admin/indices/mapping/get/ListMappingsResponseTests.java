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
