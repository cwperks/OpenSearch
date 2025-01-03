/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins;

import org.opensearch.plugins.resource.SharableResourceType;

/**
 * A ResourceAccessControlPlugin is responsible for assigned a resource sharing service for each
 * {@link SharableResourceType} that is registered by a {@link ResourcePlugin}. There can only be a single
 * ResourceAccessControlPlugin installed.
 */
public interface ResourceAccessControlPlugin {
    void assignResourceSharingService(SharableResourceType resourceType);
}
