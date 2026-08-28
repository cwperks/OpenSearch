/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.core.xcontent;

import org.opensearch.core.common.Strings;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;

import static org.opensearch.core.xcontent.XContentBuilderUtils.object;
import static org.opensearch.core.xcontent.XContentBuilderUtils.objectArray;

public class XContentBuilderUtilsTests extends OpenSearchTestCase {

    public void testObjectArrayWritesNestedObjects() {
        ToXContentObject content = (builder, params) -> object(builder, b -> {
            b.field("status", "ok");
            objectArray(b, "items", List.of(new Item("one", 1), new Item("two", 2)), (itemBuilder, item) -> {
                itemBuilder.field("name", item.name);
                itemBuilder.field("count", item.count);
            });
        });

        assertEquals(
            "{\"status\":\"ok\",\"items\":[{\"name\":\"one\",\"count\":1},{\"name\":\"two\",\"count\":2}]}",
            Strings.toString(MediaTypeRegistry.JSON, content)
        );
    }

    private record Item(String name, int count) {}
}
