/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.admin.cluster.snapshots.metadata;

import org.opensearch.action.ActionRunnable;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.repositories.Repository;
import org.opensearch.snapshots.SnapshotId;
import org.opensearch.snapshots.SnapshotInfo;
import org.opensearch.snapshots.SnapshotMissingException;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;

/** Reads only completed snapshot metadata, without touching shard data or cluster configuration. */
public class TransportGetSnapshotMetadataAction extends TransportClusterManagerNodeAction<
    GetSnapshotMetadataRequest,
    GetSnapshotMetadataResponse> {

    private final RepositoriesService repositoriesService;

    @Inject
    public TransportGetSnapshotMetadataAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        RepositoriesService repositoriesService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            GetSnapshotMetadataAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            GetSnapshotMetadataRequest::new,
            indexNameExpressionResolver
        );
        this.repositoriesService = repositoriesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected GetSnapshotMetadataResponse read(StreamInput in) throws IOException {
        return new GetSnapshotMetadataResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(GetSnapshotMetadataRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected void clusterManagerOperation(
        GetSnapshotMetadataRequest request,
        ClusterState state,
        ActionListener<GetSnapshotMetadataResponse> listener
    ) {
        repositoriesService.getRepositoryData(request.repository(), ActionListener.wrap(data -> {
            SnapshotId id = data.getSnapshotIds()
                .stream()
                .filter(snapshot -> snapshot.getName().equals(request.snapshot()))
                .findFirst()
                .orElseThrow(() -> new SnapshotMissingException(request.repository(), request.snapshot()));
            // Repository callbacks may run on other executors. Keep blocking blob reads off transport threads.
            threadPool.generic().execute(ActionRunnable.supply(listener, () -> {
                Repository repository = repositoriesService.repository(request.repository());
                SnapshotInfo info = repository.getSnapshotInfo(id);
                Metadata metadata = Boolean.TRUE.equals(info.includeGlobalState()) ? repository.getSnapshotGlobalMetadata(id) : null;
                return new GetSnapshotMetadataResponse(request.repository(), id.getName(), info.includeGlobalState(), metadata);
            }));
        }, listener::onFailure));
    }
}
