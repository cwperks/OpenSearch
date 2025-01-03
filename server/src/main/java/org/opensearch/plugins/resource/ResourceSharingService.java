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

    /**
     * Returns the resource type this service is responsible for.
     *
     * @return The resource type. Will match {@link SharableResourceType#getResourceType()} for the respective
     * sharable resource type
     */
    String getResourceType();

    /**
     * Checks if the resource is shared with the current requester.
     *
     * @param resourceId The resource id
     * @param shareListener The listener to be called when the check is complete
     */
    void isSharedWithCurrentRequester(String resourceId, ActionListener<Boolean> shareListener);
}
