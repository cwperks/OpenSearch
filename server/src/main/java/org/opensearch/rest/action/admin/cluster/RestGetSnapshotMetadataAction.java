/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.rest.action.admin.cluster;

import org.opensearch.action.admin.cluster.snapshots.metadata.GetSnapshotMetadataAction;
import org.opensearch.action.admin.cluster.snapshots.metadata.GetSnapshotMetadataRequest;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.transport.client.node.NodeClient;

import java.util.List;

import static org.opensearch.rest.RestRequest.Method.GET;

/** Inspects a snapshot's global metadata without restoring it. */
public class RestGetSnapshotMetadataAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/_snapshot/{repository}/{snapshot}/_metadata"));
    }

    @Override
    public String getName() {
        return "get_snapshot_metadata_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        GetSnapshotMetadataRequest metadataRequest = new GetSnapshotMetadataRequest(request.param("repository"), request.param("snapshot"));
        metadataRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", metadataRequest.clusterManagerNodeTimeout())
        );
        return channel -> client.execute(GetSnapshotMetadataAction.INSTANCE, metadataRequest, new RestToXContentListener<>(channel));
    }
}
