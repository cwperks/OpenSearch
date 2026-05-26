/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.cluster.metadata;

import org.opensearch.core.common.Strings;
import org.opensearch.search.fetch.subphase.FetchSourceContext;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helpers for alias-level source field filtering.
 *
 * @opensearch.internal
 */
public final class AliasFieldFilter {

    private AliasFieldFilter() {}

    public static AliasMetadata resolveSourceFilteringAlias(IndexMetadata indexMetadata, String... aliasNames) {
        if (aliasNames == null || aliasNames.length == 0) {
            return null;
        }

        AliasMetadata sourceFilteringAlias = null;
        for (String aliasName : aliasNames) {
            AliasMetadata aliasMetadata = indexMetadata.getAliases().get(aliasName);
            if (aliasMetadata == null || aliasMetadata.sourceFilteringRequired() == false) {
                continue;
            }

            if (sourceFilteringAlias != null) {
                throw new IllegalArgumentException(
                    "Combining multiple field-filtered aliases on index [" + indexMetadata.getIndex().getName() + "] is not supported"
                );
            }

            sourceFilteringAlias = aliasMetadata;
        }

        return sourceFilteringAlias;
    }

    public static FetchSourceContext merge(FetchSourceContext current, AliasMetadata aliasMetadata) {
        if (aliasMetadata == null || aliasMetadata.sourceFilteringRequired() == false) {
            return current;
        }

        if (current != null && current.fetchSource() == false) {
            return current;
        }

        if (current == null) {
            return new FetchSourceContext(true, aliasMetadata.filterIncludes(), aliasMetadata.filterExcludes());
        }

        return new FetchSourceContext(
            true,
            intersectIncludes(current.includes(), aliasMetadata.filterIncludes()),
            union(current.excludes(), aliasMetadata.filterExcludes())
        );
    }

    static String[] intersectIncludes(String[] left, String[] right) {
        if (left == null || left.length == 0) {
            return right == null ? Strings.EMPTY_ARRAY : right;
        }
        if (right == null || right.length == 0) {
            return left;
        }

        Set<String> intersection = new LinkedHashSet<>(Arrays.asList(left));
        intersection.retainAll(Arrays.asList(right));
        return intersection.toArray(new String[0]);
    }

    static String[] union(String[] left, String[] right) {
        if ((left == null || left.length == 0) && (right == null || right.length == 0)) {
            return Strings.EMPTY_ARRAY;
        }

        Set<String> union = new LinkedHashSet<>();
        if (left != null) {
            union.addAll(Arrays.asList(left));
        }
        if (right != null) {
            union.addAll(Arrays.asList(right));
        }
        return union.toArray(new String[0]);
    }
}
