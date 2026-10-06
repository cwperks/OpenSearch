/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.fetch.subphase.highlight;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.test.OpenSearchSingleNodeTestCase;

import java.util.List;

import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertFailures;
import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.containsString;

public class HighlightFragmentLimitTests extends OpenSearchSingleNodeTestCase {
    public void testLimitRejectsRequestsAndCanBeLowered() {
        createIndex("test", Settings.EMPTY, "_doc", "text", "type=text,term_vector=with_positions_offsets");
        client().prepareIndex("test")
            .setId("tiny")
            .setSource("text", "A needle.")
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .get();
        for (String type : List.of("plain", "unified", "fvh")) {
            for (int count : new int[] { 1001, Integer.MAX_VALUE }) {
                assertFailures(
                    client().prepareSearch("test")
                        .highlighter(
                            new HighlightBuilder().numOfFragments(count).field(new HighlightBuilder.Field("text").highlighterType(type))
                        ),
                    RestStatus.BAD_REQUEST,
                    containsString("index.highlight.max_number_of_fragments")
                );
                assertFailures(
                    client().prepareSearch("test")
                        .highlighter(
                            new HighlightBuilder().field(new HighlightBuilder.Field("text").numOfFragments(count).highlighterType(type))
                        ),
                    RestStatus.BAD_REQUEST,
                    containsString("index.highlight.max_number_of_fragments")
                );
            }
        }
        assertAcked(
            client().admin()
                .indices()
                .prepareUpdateSettings("test")
                .setSettings(Settings.builder().put(IndexSettings.MAX_HIGHLIGHT_FRAGMENTS_SETTING.getKey(), 1))
        );
        for (String type : List.of("plain", "unified", "fvh")) {
            assertEquals(1, fragments(type, "tiny", 1));
            assertEquals(1, fragments(type, "tiny", 0));
            assertFailures(
                client().prepareSearch("test")
                    .highlighter(new HighlightBuilder().field(new HighlightBuilder.Field("text").numOfFragments(2).highlighterType(type))),
                RestStatus.BAD_REQUEST,
                containsString("must be less than or equal to [1]")
            );
        }
    }

    public void testFragmentCountsAcrossHighlighters() {
        createIndex("test", Settings.EMPTY, "_doc", "text", "type=text,term_vector=with_positions_offsets");
        String sentence =
            "A matching needle appears in this sentence with enough surrounding words to make this an independent highlighting fragment. ";
        client().prepareIndex("test")
            .setId("dense")
            .setSource("text", sentence.repeat(1200))
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .get();
        client().prepareIndex("test")
            .setId("tiny")
            .setSource("text", "A needle.")
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .get();

        for (String type : List.of("plain", "unified", "fvh")) {
            for (int count : new int[] { 100, 1000 }) {
                assertEquals(type, 1, fragments(type, "tiny", count));
                assertEquals(type, count, fragments(type, "dense", count));
            }
            assertEquals(type, 1, fragments(type, "dense", 0));
        }

        assertAcked(
            client().admin()
                .indices()
                .prepareUpdateSettings("test")
                .setSettings(Settings.builder().put(IndexSettings.MAX_HIGHLIGHT_FRAGMENTS_SETTING.getKey(), 10000))
        );
        for (String type : List.of("plain", "unified", "fvh")) {
            assertEquals(type, 1, fragments(type, "tiny", 10000));
            int actual = fragments(type, "dense", 10000);
            assertTrue(type + " should return more than the default limit after raising it", actual > 1000);
            assertTrue(actual <= 10000);
        }
    }

    private int fragments(String type, String id, int count) {
        SearchResponse response = client().prepareSearch("test")
            .setQuery(
                QueryBuilders.boolQuery().filter(QueryBuilders.idsQuery().addIds(id)).must(QueryBuilders.matchQuery("text", "needle"))
            )
            .highlighter(
                new HighlightBuilder().field(
                    // Raise FVH's independent phrase limit so it does not mask the fragment-count boundary.
                    new HighlightBuilder.Field("text").highlighterType(type).fragmentSize(100).numOfFragments(count).phraseLimit(10000)
                )
            )
            .get();
        assertNoFailures(response);
        assertEquals(1, response.getHits().getHits().length);
        return response.getHits().getAt(0).getHighlightFields().get("text").fragments().length;
    }
}
