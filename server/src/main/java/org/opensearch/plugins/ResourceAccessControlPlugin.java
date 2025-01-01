/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins;

import org.opensearch.plugins.resource.ResourceSharingService;
import org.opensearch.plugins.resource.ResourceType;

public interface ResourceAccessControlPlugin {
    ResourceSharingService getResourceSharingService(ResourceType resourceType);
}
