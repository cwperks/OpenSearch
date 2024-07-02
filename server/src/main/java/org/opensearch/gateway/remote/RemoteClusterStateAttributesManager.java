/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.gateway.remote;

import org.opensearch.action.LatchedActionListener;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.DiffableUtils;
import org.opensearch.cluster.DiffableUtils.NonDiffableValueSerializer;
import org.opensearch.common.CheckedRunnable;
import org.opensearch.common.remote.AbstractRemoteWritableBlobEntity;
import org.opensearch.common.remote.RemoteWritableEntityStore;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.gateway.remote.model.RemoteClusterBlocks;
import org.opensearch.gateway.remote.model.RemoteClusterStateBlobStore;
import org.opensearch.gateway.remote.model.RemoteClusterStateCustoms;
import org.opensearch.gateway.remote.model.RemoteDiscoveryNodes;
import org.opensearch.gateway.remote.model.RemoteReadResult;
import org.opensearch.index.translog.transfer.BlobStoreTransferService;
import org.opensearch.repositories.blobstore.BlobStoreRepository;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Manager which provides APIs to upload and download attributes of ClusterState to the {@link RemoteClusterStateBlobStore}
 *
 * @opensearch.internal
 */
public class RemoteClusterStateAttributesManager {
    public static final String CLUSTER_STATE_ATTRIBUTE = "cluster_state_attribute";
    public static final String DISCOVERY_NODES = "nodes";
    public static final String CLUSTER_BLOCKS = "blocks";
    public static final int CLUSTER_STATE_ATTRIBUTES_CURRENT_CODEC_VERSION = 1;
    private final Map<String, RemoteWritableEntityStore> remoteWritableEntityStores;
    private final NamedWriteableRegistry namedWriteableRegistry;

    RemoteClusterStateAttributesManager(
        String clusterName,
        BlobStoreRepository blobStoreRepository,
        BlobStoreTransferService blobStoreTransferService,
        NamedWriteableRegistry namedWriteableRegistry,
        ThreadPool threadpool
    ) {
        this.namedWriteableRegistry = namedWriteableRegistry;
        this.remoteWritableEntityStores = new HashMap<>();
        this.remoteWritableEntityStores.put(
            RemoteDiscoveryNodes.DISCOVERY_NODES,
            new RemoteClusterStateBlobStore<>(
                blobStoreTransferService,
                blobStoreRepository,
                clusterName,
                threadpool,
                ThreadPool.Names.REMOTE_STATE_READ
            )
        );
        this.remoteWritableEntityStores.put(
            RemoteClusterBlocks.CLUSTER_BLOCKS,
            new RemoteClusterStateBlobStore<>(
                blobStoreTransferService,
                blobStoreRepository,
                clusterName,
                threadpool,
                ThreadPool.Names.REMOTE_STATE_READ
            )
        );
        this.remoteWritableEntityStores.put(
            RemoteClusterStateCustoms.CLUSTER_STATE_CUSTOM,
            new RemoteClusterStateBlobStore<>(
                blobStoreTransferService,
                blobStoreRepository,
                clusterName,
                threadpool,
                ThreadPool.Names.REMOTE_STATE_READ
            )
        );
    }

    /**
     * Allows async upload of Cluster State Attribute components to remote
     */
    CheckedRunnable<IOException> getAsyncMetadataWriteAction(
        String component,
        AbstractRemoteWritableBlobEntity blobEntity,
        LatchedActionListener<ClusterMetadataManifest.UploadedMetadata> latchedActionListener
    ) {
        return () -> getStore(blobEntity).writeAsync(blobEntity, getActionListener(component, blobEntity, latchedActionListener));
    }

    private ActionListener<Void> getActionListener(
        String component,
        AbstractRemoteWritableBlobEntity remoteObject,
        LatchedActionListener<ClusterMetadataManifest.UploadedMetadata> latchedActionListener
    ) {
        return ActionListener.wrap(
            resp -> latchedActionListener.onResponse(remoteObject.getUploadedMetadata()),
            ex -> latchedActionListener.onFailure(new RemoteStateTransferException(component, remoteObject, ex))
        );
    }

    private RemoteWritableEntityStore getStore(AbstractRemoteWritableBlobEntity entity) {
        RemoteWritableEntityStore remoteStore = remoteWritableEntityStores.get(entity.getType());
        if (remoteStore == null) {
            throw new IllegalArgumentException("Unknown entity type [" + entity.getType() + "]");
        }
        return remoteStore;
    }

    public CheckedRunnable<IOException> getAsyncMetadataReadAction(
        String component,
        AbstractRemoteWritableBlobEntity blobEntity,
        LatchedActionListener<RemoteReadResult> listener
    ) {
        final ActionListener actionListener = ActionListener.wrap(
            response -> listener.onResponse(new RemoteReadResult(response, CLUSTER_STATE_ATTRIBUTE, component)),
            listener::onFailure
        );
        return () -> getStore(blobEntity).readAsync(blobEntity, actionListener);
    }

    public DiffableUtils.MapDiff<String, ClusterState.Custom, Map<String, ClusterState.Custom>> getUpdatedCustoms(
        ClusterState clusterState,
        ClusterState previousClusterState,
        boolean isRemotePublicationEnabled,
        boolean isFirstUpload
    ) {
        if (!isRemotePublicationEnabled) {
            // When isRemotePublicationEnabled is false, we do not want store any custom objects
            return DiffableUtils.diff(
                Collections.emptyMap(),
                Collections.emptyMap(),
                DiffableUtils.getStringKeySerializer(),
                NonDiffableValueSerializer.getAbstractInstance()
            );
        }
        if (isFirstUpload) {
            // For first upload of ephemeral metadata, we want to upload all customs
            return DiffableUtils.diff(
                Collections.emptyMap(),
                clusterState.customs(),
                DiffableUtils.getStringKeySerializer(),
                NonDiffableValueSerializer.getAbstractInstance()
            );
        }
        return DiffableUtils.diff(
            previousClusterState.customs(),
            clusterState.customs(),
            DiffableUtils.getStringKeySerializer(),
            NonDiffableValueSerializer.getAbstractInstance()
        );
    }
}
