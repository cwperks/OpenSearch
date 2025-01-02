/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource;

/**
 * Interface for a resource type
 */
public interface SharableResourceType {
    /**
     * Type of the resource. This must be unique across all registered resource types.
     * @return a string containing the type of the resource
     */
    String getResourceType();

    /**
     * The index where resource metadata is stored
     * @return the name of the index where resource metadata is stored
     */
    String getResourceIndex();

    /**
     * This method is called when initializing ResourcePlugins to assign an instance
     * of {@link ResourceSharingService} to the resource type.
     */
    void assignResourceSharingService(ResourceSharingService resourceSharingService);

    /**
     * @return returns a parser for this resource
     */
    default ResourceParser<? extends SharableResource> getResourceParser() {
        return null;
    };
}
