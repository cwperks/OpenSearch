/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins;

import org.opensearch.plugins.resource.ResourceType;

import java.util.List;

/**
 * A ResourcePlugin registers a list of {@link ResourceType}. These are resources created by the plugin
 * and typically stored in a system index. Resources are provided protection by the {@link ResourceAccessControlPlugin}.
 */
public interface ResourcePlugin {
    List<ResourceType> getResourceTypes();
}
