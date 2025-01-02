/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource.sample;

import org.opensearch.action.ActionRequest;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.ResourcePlugin;
import org.opensearch.plugins.SystemIndexPlugin;
import org.opensearch.plugins.resource.SharableResourceType;
import org.opensearch.plugins.resource.sample.action.create.CreateSampleResourceAction;
import org.opensearch.plugins.resource.sample.action.create.CreateSampleResourceRestAction;
import org.opensearch.plugins.resource.sample.action.create.CreateSampleResourceTransportAction;
import org.opensearch.plugins.resource.sample.action.get.GetSampleResourceAction;
import org.opensearch.plugins.resource.sample.action.get.GetSampleResourceRestAction;
import org.opensearch.plugins.resource.sample.action.get.GetSampleResourceTransportAction;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class SampleResourcePlugin extends Plugin implements ResourcePlugin, SystemIndexPlugin, ActionPlugin {

    public static final String RESOURCE_INDEX_NAME = ".sample_resources";

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        return List.of(new CreateSampleResourceRestAction(), new GetSampleResourceRestAction());
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
            new ActionHandler<>(CreateSampleResourceAction.INSTANCE, CreateSampleResourceTransportAction.class),
            new ActionHandler<>(GetSampleResourceAction.INSTANCE, GetSampleResourceTransportAction.class)
        );
    }

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        final SystemIndexDescriptor systemIndexDescriptor = new SystemIndexDescriptor(RESOURCE_INDEX_NAME, "Example index with resources");
        return Collections.singletonList(systemIndexDescriptor);
    }

    @Override
    public List<SharableResourceType> getResourceTypes() {
        return List.of(SampleResourceType.getInstance());
    }
}
