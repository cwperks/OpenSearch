/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.sample;

import org.opensearch.plugins.resource.ResourceSharingService;
import org.opensearch.plugins.resource.SharableResourceType;

import static org.opensearch.plugins.resource.sample.SampleResourcePlugin.RESOURCE_INDEX_NAME;

public class SampleResourceType implements SharableResourceType {
    private volatile ResourceSharingService resourceSharingService;

    private static final SampleResourceType INSTANCE = new SampleResourceType();

    private SampleResourceType() {}

    public static SampleResourceType getInstance() {
        return INSTANCE;
    }

    @Override
    public String getResourceType() {
        return "sample_resource";
    }

    @Override
    public String getResourceIndex() {
        return RESOURCE_INDEX_NAME;
    }

    @Override
    public void assignResourceSharingService(ResourceSharingService resourceSharingService) {
        this.resourceSharingService = resourceSharingService;
    }

    public ResourceSharingService getResourceSharingService() {
        return this.resourceSharingService;
    }
}
