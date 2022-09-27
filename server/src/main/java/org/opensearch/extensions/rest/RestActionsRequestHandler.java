/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.extensions.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.authz.AuthorizationRequest;
import org.opensearch.authz.AuthorizationResponse;
import org.opensearch.authz.AuthorizationStatus;
import org.opensearch.extensions.DiscoveryExtension;
import org.opensearch.extensions.ExtensionStringResponse;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.transport.TransportResponse;
import org.opensearch.transport.TransportService;

import java.util.Map;

/**
 * Handles requests to register extension REST actions.
 *
 * @opensearch.internal
 */
public class RestActionsRequestHandler {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final RestController restController;
    private final Map<String, DiscoveryExtension> extensionIdMap;
    private final TransportService transportService;

    /**
     * Instantiates a new REST Actions Request Handler using the Node's RestController.
     *
     * @param restController  The Node's {@link RestController}.
     * @param extensionIdMap  A map of extension uniqueId to DiscoveryExtension
     * @param transportService  The Node's transportService
     */
    public RestActionsRequestHandler(
        RestController restController,
        Map<String, DiscoveryExtension> extensionIdMap,
        TransportService transportService
    ) {
        this.restController = restController;
        this.extensionIdMap = extensionIdMap;
        this.transportService = transportService;
    }

    /**
     * Handles a {@link RegisterRestActionsRequest}.
     *
     * @param restActionsRequest  The request to handle.
     * @return A {@link ExtensionStringResponse} indicating success.
     * @throws Exception if the request is not handled properly.
     */
    public TransportResponse handleRegisterRestActionsRequest(RegisterRestActionsRequest restActionsRequest) throws Exception {
        DiscoveryExtension discoveryExtension = extensionIdMap.get(restActionsRequest.getUniqueId());
        RestHandler handler = new RestSendToExtensionAction(restActionsRequest, discoveryExtension, transportService);
        restController.registerHandler(handler);
        return new ExtensionStringResponse(
            "Registered extension " + restActionsRequest.getUniqueId() + " to handle REST Actions " + restActionsRequest.getRestActions()
        );
    }

    /**
     * Handles a {@link RegisterRestActionsRequest}.
     *
     * @param authRequest  The request to handle.
     * @return A {@link AuthorizationResponse} indicating GRANTED OR DENIED.
     * @throws Exception if the request is not handled properly.
     */
    public TransportResponse handleAuthorizationRequest(AuthorizationRequest authRequest) throws Exception {
        // TODO Implement privilege evaluation - This is implemented as a separate request here, but
        // should be moved to a central location like RestSendToExtensionAction.prepareRequest or similar
        // to the SecurityFilter today

        // 1. Get subject of the request
        // 2. Verify isAuthenticated
        // 3. Resolve subject -> List<Permission>
        // 4. Evaluate privileges

        logger.info("AuthorizationRequest Permission ID: " + authRequest.getPermissionId());
        logger.info("AuthorizationRequest Request Issuer Identity: " + authRequest.getRequestIssuerIdentity());
        logger.info("AuthorizationRequest Checkable Parameters: " + authRequest.getParams());

        return new AuthorizationResponse(
            "Authorized",
            AuthorizationStatus.GRANTED
        );
    }
}
