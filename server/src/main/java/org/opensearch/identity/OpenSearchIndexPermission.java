/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import java.util.List;

public class OpenSearchIndexPermission {
    public List<String> indexPatterns;

    public List<OpenSearchWildcardPermission> actionPermissions;

    public OpenSearchIndexPermission(List<String> indexPatterns, List<OpenSearchWildcardPermission> actionPermissions) {
        this.indexPatterns = indexPatterns;
        this.actionPermissions = actionPermissions;
    }
}
