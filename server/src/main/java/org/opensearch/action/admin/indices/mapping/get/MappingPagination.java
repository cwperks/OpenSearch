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
import org.opensearch.action.pagination.PageToken;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Stateless cursor pagination for the concrete indices selected by a list mappings request.
 *
 * @opensearch.internal
 */
public final class MappingPagination {

    public static final String PAGINATED_ENTITY = "mappings";
    private static final String TOKEN_SEPARATOR = "\n";

    private MappingPagination() {}

    public static Page paginate(String[] concreteIndices, PageParams pageParams) {
        Objects.requireNonNull(concreteIndices, "concrete indices must not be null");
        Objects.requireNonNull(pageParams, "page parameters must not be null");

        final String lastIndex = decodeToken(pageParams.getRequestedToken(), pageParams.getSort());
        final Comparator<String> comparator = PageParams.PARAM_ASC_SORT_VALUE.equals(pageParams.getSort())
            ? Comparator.naturalOrder()
            : Comparator.reverseOrder();

        final List<String> candidates = Arrays.stream(concreteIndices)
            .distinct()
            .filter(index -> lastIndex == null || comparator.compare(index, lastIndex) > 0)
            .sorted(comparator)
            .limit((long) pageParams.getSize() + 1)
            .toList();

        final boolean hasNextPage = candidates.size() > pageParams.getSize();
        final int pageSize = hasNextPage ? pageParams.getSize() : candidates.size();
        final List<String> pageIndices = new ArrayList<>(candidates.subList(0, pageSize));
        final String nextToken = hasNextPage ? encodeToken(pageParams.getSort(), pageIndices.get(pageSize - 1)) : null;

        return new Page(pageIndices, new PageToken(nextToken, PAGINATED_ENTITY));
    }

    public static void validateToken(String token, String sort) {
        decodeToken(token, sort);
    }

    private static String encodeToken(String sort, String lastIndex) {
        final String token = sort + TOKEN_SEPARATOR + lastIndex;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeToken(String token, String sort) {
        if (token == null) {
            return null;
        }
        try {
            final String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            final int separator = decoded.indexOf(TOKEN_SEPARATOR);
            if (separator <= 0 || separator == decoded.length() - 1) {
                throw invalidToken();
            }
            if (sort.equals(decoded.substring(0, separator)) == false) {
                throw new OpenSearchParseException("next_token sort does not match the request sort");
            }
            return decoded.substring(separator + TOKEN_SEPARATOR.length());
        } catch (IllegalArgumentException e) {
            throw invalidToken();
        }
    }

    private static OpenSearchParseException invalidToken() {
        return new OpenSearchParseException("Parameter [next_token] is invalid");
    }

    public static class Page {
        private final List<String> indices;
        private final PageToken pageToken;

        Page(List<String> indices, PageToken pageToken) {
            this.indices = List.copyOf(indices);
            this.pageToken = pageToken;
        }

        public List<String> indices() {
            return indices;
        }

        public PageToken pageToken() {
            return pageToken;
        }
    }
}
