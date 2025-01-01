/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource;

import org.opensearch.core.action.ActionListener;

/**
 * Interface for Resource Sharing Service
 */
public interface ResourceSharingService {
    ResourceType getResourceType();

    void isSharedWithCurrentRequester(String resourceId, ActionListener<Boolean> shareListener);
}
