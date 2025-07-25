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

package org.opensearch.index.query;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.opensearch.common.lucene.search.Queries;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.ParsingException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ObjectParser;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.mapper.MappedFieldType;
import org.opensearch.index.mapper.NumberFieldMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.opensearch.common.lucene.search.Queries.fixNegativeQueryIfNeeded;

/**
 * A Query that matches documents matching boolean combinations of other queries.
 *
 * @opensearch.internal
 */
public class BoolQueryBuilder extends AbstractQueryBuilder<BoolQueryBuilder> {
    public static final String NAME = "bool";

    public static final boolean ADJUST_PURE_NEGATIVE_DEFAULT = true;

    private static final ParseField MUST_NOT = new ParseField("must_not").withDeprecation("mustNot");
    private static final ParseField FILTER = new ParseField("filter");
    private static final ParseField SHOULD = new ParseField("should");
    private static final ParseField MUST = new ParseField("must");
    private static final ParseField MINIMUM_SHOULD_MATCH = new ParseField("minimum_should_match");
    private static final ParseField ADJUST_PURE_NEGATIVE = new ParseField("adjust_pure_negative");

    private final List<QueryBuilder> mustClauses = new ArrayList<>();

    private final List<QueryBuilder> mustNotClauses = new ArrayList<>();

    private final List<QueryBuilder> filterClauses = new ArrayList<>();

    private final List<QueryBuilder> shouldClauses = new ArrayList<>();

    private boolean adjustPureNegative = ADJUST_PURE_NEGATIVE_DEFAULT;

    private String minimumShouldMatch;

    /**
     * Build an empty bool query.
     */
    public BoolQueryBuilder() {}

    /**
     * Read from a stream.
     */
    public BoolQueryBuilder(StreamInput in) throws IOException {
        super(in);
        mustClauses.addAll(readQueries(in));
        mustNotClauses.addAll(readQueries(in));
        shouldClauses.addAll(readQueries(in));
        filterClauses.addAll(readQueries(in));
        adjustPureNegative = in.readBoolean();
        minimumShouldMatch = in.readOptionalString();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        writeQueries(out, mustClauses);
        writeQueries(out, mustNotClauses);
        writeQueries(out, shouldClauses);
        writeQueries(out, filterClauses);
        out.writeBoolean(adjustPureNegative);
        out.writeOptionalString(minimumShouldMatch);
    }

    /**
     * Adds a query that <b>must</b> appear in the matching documents and will
     * contribute to scoring. No {@code null} value allowed.
     */
    public BoolQueryBuilder must(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("inner bool query clause cannot be null");
        }
        mustClauses.add(queryBuilder);
        return this;
    }

    /**
     * Gets the queries that <b>must</b> appear in the matching documents.
     */
    public List<QueryBuilder> must() {
        return this.mustClauses;
    }

    /**
     * Adds a query that <b>must</b> appear in the matching documents but will
     * not contribute to scoring. If null value passed, then do nothing and return.
     * @param filter the filter to add to the current ConstantScoreQuery
     * @return query builder with filter combined
     */
    public BoolQueryBuilder filter(QueryBuilder filter) {
        if (validateFilterParams(filter) == false) {
            return this;
        }
        filterClauses.add(filter);
        return this;
    }

    /**
     * Gets the queries that <b>must</b> appear in the matching documents but don't contribute to scoring
     */
    public List<QueryBuilder> filter() {
        return this.filterClauses;
    }

    /**
     * Adds a query that <b>must not</b> appear in the matching documents.
     * No {@code null} value allowed.
     */
    public BoolQueryBuilder mustNot(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("inner bool query clause cannot be null");
        }
        mustNotClauses.add(queryBuilder);
        return this;
    }

    /**
     * Gets the queries that <b>must not</b> appear in the matching documents.
     */
    public List<QueryBuilder> mustNot() {
        return this.mustNotClauses;
    }

    /**
     * Adds a clause that <i>should</i> be matched by the returned documents. For a boolean query with no
     * {@code MUST} clauses one or more <code>SHOULD</code> clauses must match a document
     * for the BooleanQuery to match. No {@code null} value allowed.
     *
     * @see #minimumShouldMatch(int)
     */
    public BoolQueryBuilder should(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException("inner bool query clause cannot be null");
        }
        shouldClauses.add(queryBuilder);
        return this;
    }

    /**
     * Gets the list of clauses that <b>should</b> be matched by the returned documents.
     *
     * @see #should(QueryBuilder)
     *  @see #minimumShouldMatch(int)
     */
    public List<QueryBuilder> should() {
        return this.shouldClauses;
    }

    /**
     * @return the string representation of the minimumShouldMatch settings for this query
     */
    public String minimumShouldMatch() {
        return this.minimumShouldMatch;
    }

    /**
     * Sets the minimum should match parameter using the special syntax (for example, supporting percentage).
     * @see BoolQueryBuilder#minimumShouldMatch(int)
     */
    public BoolQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
        return this;
    }

    /**
     * Specifies a minimum number of the optional (should) boolean clauses which must be satisfied.
     * <p>
     * By default no optional clauses are necessary for a match
     * (unless there are no required clauses).  If this method is used,
     * then the specified number of clauses is required.
     * <p>
     * Use of this method is totally independent of specifying that
     * any specific clauses are required (or prohibited).  This number will
     * only be compared against the number of matching optional clauses.
     *
     * @param minimumShouldMatch the number of optional clauses that must match
     */
    public BoolQueryBuilder minimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = Integer.toString(minimumShouldMatch);
        return this;
    }

    /**
     * Returns <code>true</code> iff this query builder has at least one should, must, must not or filter clause.
     * Otherwise <code>false</code>.
     */
    public boolean hasClauses() {
        return !(mustClauses.isEmpty() && shouldClauses.isEmpty() && mustNotClauses.isEmpty() && filterClauses.isEmpty());
    }

    /**
     * If a boolean query contains only negative ("must not") clauses should the
     * BooleanQuery be enhanced with a {@link MatchAllDocsQuery} in order to act
     * as a pure exclude. The default is <code>true</code>.
     */
    public BoolQueryBuilder adjustPureNegative(boolean adjustPureNegative) {
        this.adjustPureNegative = adjustPureNegative;
        return this;
    }

    /**
     * @return the setting for the adjust_pure_negative setting in this query
     */
    public boolean adjustPureNegative() {
        return this.adjustPureNegative;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        doXArrayContent(MUST, mustClauses, builder, params);
        doXArrayContent(FILTER, filterClauses, builder, params);
        doXArrayContent(MUST_NOT, mustNotClauses, builder, params);
        doXArrayContent(SHOULD, shouldClauses, builder, params);
        builder.field(ADJUST_PURE_NEGATIVE.getPreferredName(), adjustPureNegative);
        if (minimumShouldMatch != null) {
            builder.field(MINIMUM_SHOULD_MATCH.getPreferredName(), minimumShouldMatch);
        }
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    private static void doXArrayContent(ParseField field, List<QueryBuilder> clauses, XContentBuilder builder, Params params)
        throws IOException {
        if (clauses.isEmpty()) {
            return;
        }
        builder.startArray(field.getPreferredName());
        for (QueryBuilder clause : clauses) {
            clause.toXContent(builder, params);
        }
        builder.endArray();
    }

    private static final ObjectParser<BoolQueryBuilder, Void> PARSER = new ObjectParser<>("bool", BoolQueryBuilder::new);
    static {
        PARSER.declareObjectArrayOrNull((builder, clauses) -> clauses.forEach(builder::must), (p, c) -> parseInnerQueryBuilder(p), MUST);
        PARSER.declareObjectArrayOrNull(
            (builder, clauses) -> clauses.forEach(builder::should),
            (p, c) -> parseInnerQueryBuilder(p),
            SHOULD
        );
        PARSER.declareObjectArrayOrNull(
            (builder, clauses) -> clauses.forEach(builder::mustNot),
            (p, c) -> parseInnerQueryBuilder(p),
            MUST_NOT
        );
        PARSER.declareObjectArrayOrNull(
            (builder, clauses) -> clauses.forEach(builder::filter),
            (p, c) -> parseInnerQueryBuilder(p),
            FILTER
        );
        PARSER.declareBoolean(BoolQueryBuilder::adjustPureNegative, ADJUST_PURE_NEGATIVE);
        PARSER.declareField(
            BoolQueryBuilder::minimumShouldMatch,
            (p, c) -> p.textOrNull(),
            MINIMUM_SHOULD_MATCH,
            ObjectParser.ValueType.VALUE
        );
        PARSER.declareString(BoolQueryBuilder::queryName, NAME_FIELD);
        PARSER.declareFloat(BoolQueryBuilder::boost, BOOST_FIELD);
    }

    public static BoolQueryBuilder fromXContent(XContentParser parser) throws IOException, ParsingException {
        return PARSER.parse(parser, null);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
        addBooleanClauses(context, booleanQueryBuilder, mustClauses, BooleanClause.Occur.MUST);
        addBooleanClauses(context, booleanQueryBuilder, mustNotClauses, BooleanClause.Occur.MUST_NOT);
        addBooleanClauses(context, booleanQueryBuilder, shouldClauses, BooleanClause.Occur.SHOULD);
        addBooleanClauses(context, booleanQueryBuilder, filterClauses, BooleanClause.Occur.FILTER);
        BooleanQuery booleanQuery = booleanQueryBuilder.build();
        if (booleanQuery.clauses().isEmpty()) {
            return new MatchAllDocsQuery();
        }

        Query query = Queries.applyMinimumShouldMatch(booleanQuery, minimumShouldMatch);
        return adjustPureNegative ? fixNegativeQueryIfNeeded(query) : query;
    }

    private static void addBooleanClauses(
        QueryShardContext context,
        BooleanQuery.Builder booleanQueryBuilder,
        List<QueryBuilder> clauses,
        Occur occurs
    ) throws IOException {
        for (QueryBuilder query : clauses) {
            Query luceneQuery = query.toQuery(context);
            booleanQueryBuilder.add(new BooleanClause(luceneQuery, occurs));
        }
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(adjustPureNegative, minimumShouldMatch, mustClauses, shouldClauses, mustNotClauses, filterClauses);
    }

    @Override
    protected boolean doEquals(BoolQueryBuilder other) {
        return Objects.equals(adjustPureNegative, other.adjustPureNegative)
            && Objects.equals(minimumShouldMatch, other.minimumShouldMatch)
            && Objects.equals(mustClauses, other.mustClauses)
            && Objects.equals(shouldClauses, other.shouldClauses)
            && Objects.equals(mustNotClauses, other.mustNotClauses)
            && Objects.equals(filterClauses, other.filterClauses);
    }

    @Override
    protected QueryBuilder doRewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        BoolQueryBuilder newBuilder = new BoolQueryBuilder();
        boolean changed = false;
        final int clauses = mustClauses.size() + mustNotClauses.size() + filterClauses.size() + shouldClauses.size();
        if (clauses == 0) {
            return new MatchAllQueryBuilder().boost(boost()).queryName(queryName());
        }
        changed |= rewriteClauses(queryRewriteContext, mustClauses, newBuilder::must);
        changed |= rewriteClauses(queryRewriteContext, mustNotClauses, newBuilder::mustNot);
        changed |= rewriteClauses(queryRewriteContext, filterClauses, newBuilder::filter);
        changed |= rewriteClauses(queryRewriteContext, shouldClauses, newBuilder::should);
        // early termination when must clause is empty and optional clauses is returning MatchNoneQueryBuilder
        if (mustClauses.size() == 0
            && filterClauses.size() == 0
            && shouldClauses.size() > 0
            && newBuilder.shouldClauses.stream().allMatch(b -> b instanceof MatchNoneQueryBuilder)) {
            return new MatchNoneQueryBuilder();
        }

        // lets do some early termination and prevent any kind of rewriting if we have a mandatory query that is a MatchNoneQueryBuilder
        Optional<QueryBuilder> any = Stream.concat(newBuilder.mustClauses.stream(), newBuilder.filterClauses.stream())
            .filter(b -> b instanceof MatchNoneQueryBuilder)
            .findAny();
        if (any.isPresent()) {
            return any.get();
        }

        changed |= rewriteMustNotRangeClausesToShould(newBuilder, queryRewriteContext);
        changed |= rewriteMustClausesToFilter(newBuilder, queryRewriteContext);

        if (changed) {
            newBuilder.adjustPureNegative = adjustPureNegative;
            if (minimumShouldMatch != null) {
                newBuilder.minimumShouldMatch = minimumShouldMatch;
            }
            newBuilder.boost(boost());
            newBuilder.queryName(queryName());
            return newBuilder;
        }
        return this;
    }

    @Override
    protected void extractInnerHitBuilders(Map<String, InnerHitContextBuilder> innerHits) {
        List<QueryBuilder> clauses = new ArrayList<>(filter());
        clauses.addAll(must());
        clauses.addAll(should());
        // no need to include must_not (since there will be no hits for it)
        for (QueryBuilder clause : clauses) {
            InnerHitContextBuilder.extractInnerHits(clause, innerHits);
        }
    }

    private static boolean rewriteClauses(
        QueryRewriteContext queryRewriteContext,
        List<QueryBuilder> builders,
        Consumer<QueryBuilder> consumer
    ) throws IOException {
        boolean changed = false;
        for (QueryBuilder builder : builders) {
            QueryBuilder result = builder.rewrite(queryRewriteContext);
            if (result != builder) {
                changed = true;
            }
            consumer.accept(result);
        }
        return changed;
    }

    @Override
    public void visit(QueryBuilderVisitor visitor) {
        visitor.accept(this);
        if (mustClauses.isEmpty() == false) {
            QueryBuilderVisitor subVisitor = visitor.getChildVisitor(Occur.MUST);
            for (QueryBuilder mustClause : mustClauses) {
                mustClause.visit(subVisitor);
            }
        }
        if (shouldClauses.isEmpty() == false) {
            QueryBuilderVisitor subVisitor = visitor.getChildVisitor(Occur.SHOULD);
            for (QueryBuilder shouldClause : shouldClauses) {
                shouldClause.visit(subVisitor);
            }
        }
        if (mustNotClauses.isEmpty() == false) {
            QueryBuilderVisitor subVisitor = visitor.getChildVisitor(Occur.MUST_NOT);
            for (QueryBuilder mustNotClause : mustNotClauses) {
                mustNotClause.visit(subVisitor);
            }
        }
        if (filterClauses.isEmpty() == false) {
            QueryBuilderVisitor subVisitor = visitor.getChildVisitor(Occur.FILTER);
            for (QueryBuilder filterClause : filterClauses) {
                filterClause.visit(subVisitor);
            }
        }

    }

    private boolean rewriteMustNotRangeClausesToShould(BoolQueryBuilder newBuilder, QueryRewriteContext queryRewriteContext) {
        // If there is a range query on a given field in a must_not clause, it's more performant to execute it as
        // multiple should clauses representing everything outside the target range.

        // First check if we can get the individual LeafContexts. If we can't, we can't proceed with the rewrite, since we can't confirm
        // every doc has exactly 1 value for this field.
        List<LeafReaderContext> leafReaderContexts = getLeafReaderContexts(queryRewriteContext);
        if (leafReaderContexts == null || leafReaderContexts.isEmpty()) {
            return false;
        }

        QueryShardContext shardContext = getQueryShardContext(queryRewriteContext);

        boolean changed = false;
        // For now, only handle the case where there's exactly 1 complement-aware query for this field.
        Map<String, Integer> fieldCounts = new HashMap<>();
        Set<ComplementAwareQueryBuilder> complementAwareQueries = new HashSet<>();
        for (QueryBuilder clause : mustNotClauses) {
            if (clause instanceof ComplementAwareQueryBuilder && clause instanceof WithFieldName wfn) {
                fieldCounts.merge(wfn.fieldName(), 1, Integer::sum);
                complementAwareQueries.add((ComplementAwareQueryBuilder) wfn);
            }
        }

        for (ComplementAwareQueryBuilder caq : complementAwareQueries) {
            String fieldName = ((WithFieldName) caq).fieldName();
            if (fieldCounts.getOrDefault(fieldName, 0) == 1) {
                // Check that all docs on this field have exactly 1 value, otherwise we can't perform this rewrite
                if (checkAllDocsHaveOneValue(leafReaderContexts, fieldName)) {
                    List<? extends QueryBuilder> complement = caq.getComplement(shardContext);
                    if (complement != null) {
                        BoolQueryBuilder nestedBoolQuery = new BoolQueryBuilder();
                        nestedBoolQuery.minimumShouldMatch(1);
                        for (QueryBuilder complementComponent : complement) {
                            nestedBoolQuery.should(complementComponent);
                        }
                        newBuilder.must(nestedBoolQuery);
                        newBuilder.mustNotClauses.remove(caq);
                        changed = true;
                    }
                }
            }
        }

        if (minimumShouldMatch == null && changed) {
            if ((!shouldClauses.isEmpty()) && mustClauses.isEmpty() && filterClauses.isEmpty()) {
                // If there were originally should clauses and no must/filter clauses, null minimumShouldMatch is set to a default of 1
                // within Lucene.
                // But if there was originally a must or filter clause, the default is 0.
                // If we added a must clause due to this rewrite, we should respect what the original default would have been.
                newBuilder.minimumShouldMatch(1);
            }
        }
        return changed;
    }

    private List<LeafReaderContext> getLeafReaderContexts(QueryRewriteContext queryRewriteContext) {
        if (queryRewriteContext == null) return null;
        QueryShardContext shardContext = queryRewriteContext.convertToShardContext();
        if (shardContext == null) return null;
        IndexSearcher indexSearcher = shardContext.searcher();
        if (indexSearcher == null) return null;
        return indexSearcher.getIndexReader().leaves();
    }

    private QueryShardContext getQueryShardContext(QueryRewriteContext queryRewriteContext) {
        return queryRewriteContext == null ? null : queryRewriteContext.convertToShardContext(); // Note this can still be null
    }

    private boolean checkAllDocsHaveOneValue(List<LeafReaderContext> contexts, String fieldName) {
        for (LeafReaderContext lrc : contexts) {
            PointValues values;
            try {
                LeafReader reader = lrc.reader();
                values = reader.getPointValues(fieldName);
                if (values == null || !(values.getDocCount() == reader.maxDoc() && values.getDocCount() == values.size())) {
                    return false;
                }
            } catch (IOException e) {
                // If we can't get PointValues to check on the number of values per doc, assume the query is ineligible
                return false;
            }
        }
        return true;
    }

    private boolean rewriteMustClausesToFilter(BoolQueryBuilder newBuilder, QueryRewriteContext queryRewriteContext) {
        // If we have must clauses which return the same score for all matching documents, like numeric term queries or ranges,
        // moving them from must clauses to filter clauses improves performance in some cases.
        // This works because it can let Lucene use MaxScoreCache to skip non-competitive docs.
        boolean changed = false;
        Set<QueryBuilder> mustClausesToMove = new HashSet<>();

        QueryShardContext shardContext;
        if (queryRewriteContext == null) {
            shardContext = null;
        } else {
            shardContext = queryRewriteContext.convertToShardContext(); // can still be null
        }

        for (QueryBuilder clause : mustClauses) {
            if (isClauseIrrelevantToScoring(clause, shardContext)) {
                mustClausesToMove.add(clause);
                changed = true;
            }
        }

        newBuilder.mustClauses.removeAll(mustClausesToMove);
        newBuilder.filterClauses.addAll(mustClausesToMove);
        return changed;
    }

    private boolean isClauseIrrelevantToScoring(QueryBuilder clause, QueryShardContext context) {
        // This is an incomplete list of clauses this might apply for; it can be expanded in future.

        // If a clause is purely numeric, for example a date range, its score is unimportant as
        // it'll be the same for all returned docs
        if (clause instanceof RangeQueryBuilder) return true;
        if (clause instanceof GeoBoundingBoxQueryBuilder) return true;

        // Further optimizations depend on knowing whether the field is numeric.
        // QueryBuilder.doRewrite() is called several times in the search flow, and the shard context telling us this
        // is only available the last time, when it's called from SearchService.executeQueryPhase().
        // Skip moving these clauses if we don't have the shard context.
        if (context == null) return false;
        if (!(clause instanceof WithFieldName wfn)) return false;
        MappedFieldType fieldType = context.fieldMapper(wfn.fieldName());
        if (!(fieldType instanceof NumberFieldMapper.NumberFieldType)) return false;

        if (clause instanceof MatchQueryBuilder) return true;
        if (clause instanceof TermQueryBuilder) return true;
        if (clause instanceof TermsQueryBuilder) return true;
        return false;
    }
}
