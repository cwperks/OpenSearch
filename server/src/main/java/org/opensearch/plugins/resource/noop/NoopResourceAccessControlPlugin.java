/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.noop;

import org.opensearch.plugins.ResourceAccessControlPlugin;
import org.opensearch.plugins.resource.ResourceType;

/**
 * Noop implementation of Resource Access Control Plugin
 */
public class NoopResourceAccessControlPlugin implements ResourceAccessControlPlugin {

    @Override
    public void assignResourceSharingService(ResourceType resourceType) {
        resourceType.assignResourceSharingService(new NoopResourceSharingService(resourceType.getResourceType()));
    }
}
