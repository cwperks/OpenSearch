/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.support.nodes;

import org.opensearch.action.admin.cluster.wlm.TransportWlmStatsAction;
import org.opensearch.action.admin.cluster.wlm.WlmStatsRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.PlainActionFuture;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.test.transport.CapturingTransport;
import org.opensearch.wlm.WorkloadGroupService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class TransportWlmStatsActionTests extends TransportNodesActionTests {

    /**
     * We don't want to send discovery nodes list to each request that is sent across from the coordinator node.
     * This behavior is asserted in this test.
     */
    public void testWlmStatsActionWithRetentionOfDiscoveryNodesList() {
        WlmStatsRequest request = new WlmStatsRequest();
        Map<String, List<WlmStatsRequest>> combinedSentRequest = performWlmStatsAction(request);

        assertNotNull(combinedSentRequest);
        combinedSentRequest.forEach((node, capturedRequestList) -> {
            assertNotNull(capturedRequestList);
            capturedRequestList.forEach(sentRequest -> { assertNull(sentRequest.concreteNodes()); });
        });
    }

    private Map<String, List<WlmStatsRequest>> performWlmStatsAction(WlmStatsRequest request) {
        TransportNodesAction action = new TransportWlmStatsAction(
            THREAD_POOL,
            clusterService,
            transportService,
            mock(WorkloadGroupService.class),
            new ActionFilters(Collections.emptySet())
        );
        PlainActionFuture<WlmStatsRequest> listener = new PlainActionFuture<>();
        action.new AsyncAction(null, request, listener).start();
        Map<String, List<CapturingTransport.CapturedRequest>> capturedRequests = transport.getCapturedRequestsByTargetNodeAndClear();
        Map<String, List<WlmStatsRequest>> combinedSentRequest = new HashMap<>();

        capturedRequests.forEach((node, capturedRequestList) -> {
            List<WlmStatsRequest> sentRequestList = new ArrayList<>();

            capturedRequestList.forEach(preSentRequest -> {
                BytesStreamOutput out = new BytesStreamOutput();
                try {
                    WlmStatsRequest wlmStatsRequestFromCoordinator = (WlmStatsRequest) preSentRequest.request;
                    wlmStatsRequestFromCoordinator.writeTo(out);
                    StreamInput in = out.bytes().streamInput();
                    WlmStatsRequest wlmStatsRequest = new WlmStatsRequest(in);
                    sentRequestList.add(wlmStatsRequest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            combinedSentRequest.put(node, sentRequestList);
        });

        return combinedSentRequest;
    }
}
