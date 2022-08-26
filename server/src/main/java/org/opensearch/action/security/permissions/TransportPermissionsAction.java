/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.action.security.permissions;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.subject.Subject;
import org.opensearch.action.ActionListener;
import org.opensearch.action.RequestValidators;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ack.ClusterStateUpdateResponse;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.metadata.AliasAction;
import org.opensearch.cluster.metadata.AliasMetadata;
import org.opensearch.cluster.metadata.IndexAbstraction;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.MetadataIndexAliasesService;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.collect.ImmutableOpenMap;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.identity.MyShiroModule;
import org.opensearch.index.Index;
import org.opensearch.rest.action.admin.indices.AliasesNotFoundException;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

/**
 * Add/remove aliases action
 *
 * @opensearch.internal
 */
public class TransportPermissionsAction extends TransportClusterManagerNodeAction<PermissionsRequest, PermissionsResponse> {

    private static final Logger logger = LogManager.getLogger(TransportPermissionsAction.class);

    @Inject
    public TransportPermissionsAction(
        final TransportService transportService,
        final ClusterService clusterService,
        final ThreadPool threadPool,
        final ActionFilters actionFilters,
        final IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            PermissionsAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            PermissionsRequest::new,
            indexNameExpressionResolver
        );
    }

    @Override
    protected String executor() {
        // we go async right away...
        return ThreadPool.Names.SAME;
    }

    @Override
    protected PermissionsResponse read(StreamInput in) throws IOException {
        return new PermissionsResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(PermissionsRequest request, ClusterState state) {
        Set<String> indices = new HashSet<>();
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, indices.toArray(new String[indices.size()]));
    }

    @Override
    protected void clusterManagerOperation(
        final PermissionsRequest request,
        final ClusterState state,
        final ActionListener<PermissionsResponse> listener
    ) {
        String userId = request.userId();
        Subject mySubject = MyShiroModule.getSubjectOrInternal();
        List<String> myPermissions = MyShiroModule.getPermissionsForUser(mySubject);
        Map<String, List<String>> rolePermissions = MyShiroModule.getRolesAndPermissionsForUser(mySubject);
        String callingUserId = mySubject.getPrincipal() != null ? mySubject.getPrincipal().toString() : "cwperx";
        listener.onResponse(new PermissionsResponse(callingUserId, myPermissions, rolePermissions, true));
    }
}
