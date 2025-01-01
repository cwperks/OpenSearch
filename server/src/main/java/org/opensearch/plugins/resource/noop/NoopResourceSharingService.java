/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.noop;

import org.opensearch.core.action.ActionListener;
import org.opensearch.plugins.resource.ResourceSharingService;

/**
 * Noop implementation of Resource Sharing Service
 */
public class NoopResourceSharingService implements ResourceSharingService {

    private final String resourceType;

    public NoopResourceSharingService(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public void isSharedWithCurrentRequester(String resourceId, ActionListener<Boolean> shareListener) {
        shareListener.onResponse(Boolean.TRUE);
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }
}
