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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.search.aggregations.metrics;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.index.RandomIndexWriter;
import org.opensearch.common.CheckedConsumer;
import org.opensearch.index.mapper.MappedFieldType;
import org.opensearch.index.mapper.NumberFieldMapper;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregatorTestCase;
import org.opensearch.search.aggregations.support.AggregationInspectionHelper;
import org.opensearch.search.aggregations.support.CoreValuesSourceType;
import org.opensearch.search.aggregations.support.ValuesSourceType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.opensearch.search.aggregations.AggregationBuilders.percentiles;
import static org.hamcrest.Matchers.equalTo;

public class TDigestPercentilesAggregatorTests extends AggregatorTestCase {

    @Override
    protected AggregationBuilder createAggBuilderForTypeTest(MappedFieldType fieldType, String fieldName) {
        return new PercentilesAggregationBuilder("tdist_percentiles").field(fieldName).percentilesConfig(new PercentilesConfig.TDigest());
    }

    @Override
    protected List<ValuesSourceType> getSupportedValuesSourceTypes() {
        return Arrays.asList(CoreValuesSourceType.NUMERIC, CoreValuesSourceType.DATE, CoreValuesSourceType.BOOLEAN);
    }

    public void testNoDocs() throws IOException {
        testCase(new MatchAllDocsQuery(), iw -> {
            // Intentionally not writing any docs
        }, tdigest -> {
            assertEquals(0L, tdigest.state.size());
            assertFalse(AggregationInspectionHelper.hasValue(tdigest));
        });
    }

    public void testNoMatchingField() throws IOException {
        testCase(new MatchAllDocsQuery(), iw -> {
            iw.addDocument(singleton(new SortedNumericDocValuesField("wrong_number", 7)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("wrong_number", 1)));
        }, tdigest -> {
            assertEquals(0L, tdigest.state.size());
            assertFalse(AggregationInspectionHelper.hasValue(tdigest));
        });
    }

    public void testSomeMatchesSortedNumericDocValues() throws IOException {
        testCase(new FieldExistsQuery("number"), iw -> {
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 8)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 5)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 3)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 2)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 1)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 1)));
            iw.addDocument(singleton(new SortedNumericDocValuesField("number", 0)));
        }, tdigest -> {
            assertEquals(7L, tdigest.state.size());
            assertEquals(7L, tdigest.state.centroids().size());
            assertEquals(5.0d, tdigest.percentile(75), 0.0d);
            assertEquals("5.0", tdigest.percentileAsString(75));
            assertEquals(3.0d, tdigest.percentile(71), 0.0d);
            assertEquals("3.0", tdigest.percentileAsString(71));
            assertEquals(2.0d, tdigest.percentile(50), 0.0d);
            assertEquals("2.0", tdigest.percentileAsString(50));
            assertEquals(1.0d, tdigest.percentile(22), 0.0d);
            assertEquals("1.0", tdigest.percentileAsString(22));
            assertTrue(AggregationInspectionHelper.hasValue(tdigest));
        });
    }

    public void testSomeMatchesNumericDocValues() throws IOException {
        testCase(new FieldExistsQuery("number"), iw -> {
            iw.addDocument(singleton(new NumericDocValuesField("number", 8)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 5)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 3)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 2)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 1)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 1)));
            iw.addDocument(singleton(new NumericDocValuesField("number", 0)));
        }, tdigest -> {
            assertEquals(tdigest.state.size(), 7L);
            assertTrue(tdigest.state.centroids().size() <= 7L);
            assertEquals(8.0d, tdigest.percentile(100), 0.0d);
            assertEquals("8.0", tdigest.percentileAsString(100));
            assertEquals(8.0d, tdigest.percentile(88), 0.0d);
            assertEquals("8.0", tdigest.percentileAsString(88));
            assertEquals(1.0d, tdigest.percentile(33), 0.0d);
            assertEquals("1.0", tdigest.percentileAsString(33));
            assertEquals(1.0d, tdigest.percentile(25), 0.0d);
            assertEquals("1.0", tdigest.percentileAsString(25));
            assertEquals(0.0d, tdigest.percentile(1), 0.0d);
            assertEquals("0.0", tdigest.percentileAsString(1));
            assertTrue(AggregationInspectionHelper.hasValue(tdigest));
        });
    }

    public void testQueryFiltering() throws IOException {
        final CheckedConsumer<RandomIndexWriter, IOException> docs = iw -> {
            iw.addDocument(asList(new LongPoint("row", 7), new SortedNumericDocValuesField("number", 8)));
            iw.addDocument(asList(new LongPoint("row", 6), new SortedNumericDocValuesField("number", 5)));
            iw.addDocument(asList(new LongPoint("row", 5), new SortedNumericDocValuesField("number", 3)));
            iw.addDocument(asList(new LongPoint("row", 4), new SortedNumericDocValuesField("number", 2)));
            iw.addDocument(asList(new LongPoint("row", 3), new SortedNumericDocValuesField("number", 1)));
            iw.addDocument(asList(new LongPoint("row", 2), new SortedNumericDocValuesField("number", 1)));
            iw.addDocument(asList(new LongPoint("row", 1), new SortedNumericDocValuesField("number", 0)));
        };

        testCase(LongPoint.newRangeQuery("row", 1, 4), docs, tdigest -> {
            assertEquals(4L, tdigest.state.size());
            assertEquals(4L, tdigest.state.centroids().size());
            assertEquals(2.0d, tdigest.percentile(100), 0.0d);
            assertEquals(1.0d, tdigest.percentile(50), 0.0d);
            assertEquals(1.0d, tdigest.percentile(25), 0.0d);
            assertTrue(AggregationInspectionHelper.hasValue(tdigest));
        });

        testCase(LongPoint.newRangeQuery("row", 100, 110), docs, tdigest -> {
            assertEquals(0L, tdigest.state.size());
            assertEquals(0L, tdigest.state.centroids().size());
            assertFalse(AggregationInspectionHelper.hasValue(tdigest));
        });
    }

    public void testTdigestThenHdrSettings() throws Exception {
        int sigDigits = randomIntBetween(1, 5);
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> {
            percentiles("percentiles").compression(100.0)
                .method(PercentilesMethod.TDIGEST)
                .numberOfSignificantValueDigits(sigDigits) // <-- this should trigger an exception
                .field("value");
        });
        assertThat(
            e.getMessage(),
            equalTo("Cannot set [numberOfSignificantValueDigits] because the " + "method has already been configured for TDigest")
        );
    }

    private void testCase(
        Query query,
        CheckedConsumer<RandomIndexWriter, IOException> buildIndex,
        Consumer<InternalTDigestPercentiles> verify
    ) throws IOException {
        try (Directory directory = newDirectory()) {
            try (RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory)) {
                buildIndex.accept(indexWriter);
            }

            try (IndexReader indexReader = DirectoryReader.open(directory)) {
                IndexSearcher indexSearcher = newSearcher(indexReader, true, true);

                PercentilesAggregationBuilder builder;
                // TODO this randomization path should be removed when the old settings are removed
                if (randomBoolean()) {
                    builder = new PercentilesAggregationBuilder("test").field("number").method(PercentilesMethod.TDIGEST);
                } else {
                    PercentilesConfig hdr = new PercentilesConfig.TDigest();
                    builder = new PercentilesAggregationBuilder("test").field("number").percentilesConfig(hdr);
                }

                MappedFieldType fieldType = new NumberFieldMapper.NumberFieldType("number", NumberFieldMapper.NumberType.LONG);
                TDigestPercentilesAggregator aggregator = createAggregator(builder, indexSearcher, fieldType);
                aggregator.preCollection();
                indexSearcher.search(query, aggregator);
                aggregator.postCollection();
                verify.accept((InternalTDigestPercentiles) aggregator.buildAggregation(0L));
            }
        }
    }
}
