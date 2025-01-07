/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchException;
import org.opensearch.plugins.ResourceAccessControlPlugin;
import org.opensearch.plugins.ResourcePlugin;
import org.opensearch.plugins.resource.noop.NoopResourceAccessControlPlugin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to get the current ResourcePlugin to perform authorization.
 *
 * @opensearch.experimental
 */
public class ResourceService {
    private static final Logger log = LogManager.getLogger(ResourceService.class);

    private final ResourceAccessControlPlugin controlPlugin;

    public ResourceService(final List<ResourceAccessControlPlugin> plugins) {

        if (plugins.isEmpty()) {
            log.debug("resource access control plugins size is 0. Using noop.");
            controlPlugin = new NoopResourceAccessControlPlugin();
        } else if (plugins.size() == 1) {
            log.debug("resource access control plugin installed.");
            controlPlugin = plugins.get(0);
        } else {
            throw new OpenSearchException(
                "Multiple resource access control plugins are not supported, found: "
                    + plugins.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(","))
            );
        }
    }

    public void initializeResourcePlugins(final List<ResourcePlugin> resourcePlugins) {
        if (resourcePlugins != null) {
            for (ResourcePlugin plugin : resourcePlugins) {
                List<SharableResourceType> pluginSharableResourceTypes = plugin.getResourceTypes();
                for (SharableResourceType resourceType : pluginSharableResourceTypes) {
                    controlPlugin.assignResourceSharingService(resourceType);
                }
            }
        }
    }
}
