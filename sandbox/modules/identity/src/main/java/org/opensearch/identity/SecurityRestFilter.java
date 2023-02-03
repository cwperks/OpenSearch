/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authc.AuthenticationException;
import org.opensearch.OpenSearchException;
import org.opensearch.authn.Identity;
import org.opensearch.authn.Subject;
import org.opensearch.identity.extensions.ExtensionSecurityConfigStore;
import org.opensearch.identity.jwt.JwtVendor;
import org.opensearch.authn.tokens.AuthenticationToken;
import org.opensearch.authn.tokens.BasicAuthToken;
import org.opensearch.authn.tokens.BearerAuthToken;
import org.opensearch.authn.tokens.HttpHeaderToken;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SecurityRestFilter {

    protected final Logger log = LogManager.getLogger(this.getClass());
    private final ThreadContext threadContext;
    private final Settings settings;
    private final Path configPath;

    public SecurityRestFilter(final ThreadPool threadPool, final Settings settings, final Path configPath) {
        super();
        this.threadContext = threadPool.getThreadContext();
        this.settings = settings;
        this.configPath = configPath;
    }

    /**
     * This function wraps around all rest requests
     * If the request is authenticated, then it goes through
     */
    public RestHandler wrap(RestHandler original) {
        return new RestHandler() {

            @Override
            public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
                org.apache.logging.log4j.ThreadContext.clearAll();
                if (checkAndAuthenticateRequest(request, channel, client)) {
                    String authTokenHeader = threadContext.getHeader(ThreadContextConstants.OPENSEARCH_AUTHENTICATION_TOKEN_HEADER);
                    Map<String, String> jwtClaims = new HashMap<>();
                    String encodedJwt = null;

                    if (authTokenHeader == null) {
                        Subject currentSubject = Identity.getAuthManager().getSubject();
                        // TODO replace with Principal Identifier Token if destination is extension
                        jwtClaims.put("sub", currentSubject.getPrincipal().getName());
                        jwtClaims.put("iat", Instant.now().toString());
                    }

                    if (request.path().startsWith("/_extensions/")) {
                        // TODO Figure out better way of extracting extensionId
                        // Extension routes look like: /_extensions/_opensearch-sdk-java-1/hello where
                        // opensearch-sdk-java-1 is the extensionId
                        String extensionRoute = request.path().replace("/_extensions/", "");
                        String extensionId = extensionRoute.substring(0, extensionRoute.indexOf("/"));
                        extensionId = StringUtils.strip(extensionId, "_");
                        String extensionSigningKey = ExtensionSecurityConfigStore.getInstance().getSigningKeyForExtension(extensionId);
                        if (extensionSigningKey != null) {
                            encodedJwt = JwtVendor.createJwt(jwtClaims, extensionSigningKey);
                        }
                    } else {
                        String signingKey = settings.get(ConfigConstants.IDENTITY_SIGNING_KEY);
                        if (signingKey != null) {
                            encodedJwt = JwtVendor.createJwt(jwtClaims, settings.get(ConfigConstants.IDENTITY_SIGNING_KEY));
                        }
                    }

                    String requestInfo = String.format(
                        Locale.ROOT,
                        "(nodeName=%s, requestId=%s, path=%s, jwtClaims=%s checkAndAuthenticateRequest)",
                        client.getLocalNodeId(),
                        request.getRequestId(),
                        request.path(),
                        jwtClaims
                    );

                    if (log.isDebugEnabled()) {
                        log.debug(requestInfo);
                        String logMsg = String.format(Locale.ROOT, "Created internal access token %s", encodedJwt);
                        log.debug("{} {}", requestInfo, logMsg);
                    }
                    threadContext.putHeader(ThreadContextConstants.OPENSEARCH_AUTHENTICATION_TOKEN_HEADER, encodedJwt);
                    original.handleRequest(request, channel, client);
                }
            }
        };
    }

    // True is authenticated, false if not - this is opposite of the Security plugin
    private boolean checkAndAuthenticateRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        if (!authenticate(request, channel)) {
            final OpenSearchException exc = new OpenSearchException("Authentication failed");
            channel.sendResponse(new BytesRestResponse(channel, RestStatus.UNAUTHORIZED, exc));
            return false;
        }

        return true;
    }

    /**
     * Authenticates the subject of the incoming REST request based on the auth header
     * @param request the request whose subject is to be authenticated
     * @param channel the channel to send the response on
     * @return true if authentication was successful, false otherwise
     * @throws IOException when an exception is raised writing response to channel
     */
    private boolean authenticate(RestRequest request, RestChannel channel) throws IOException {

        final Optional<String> authHeader = request.getHeaders()
            .getOrDefault(HttpHeaderToken.HEADER_NAME, Collections.emptyList())
            .stream()
            .findFirst();

        Subject subject = null;

        AuthenticationToken headerToken = null;

        if (authHeader.isPresent()) {
            try {
                headerToken = tokenType(authHeader.get());
                subject = Identity.getAuthManager().getSubject();
                if (subject != null) {
                    subject.login(headerToken);
                }
                log.info("Authentication successful");
                return true;
            } catch (final AuthenticationException ae) {
                log.info("Authentication finally failed: {}", ae.getMessage());
                return false;
            }
        }

        // TODO: Handle anonymous Auth - Allowed or Disallowed (set by the user of the system) - 401 or Login-redirect ??

        /*
        TODO: Uncomment this once it is decided to proceed with this workflow
        logger.info("Authentication unsuccessful: Missing Authentication Header");
        final BytesRestResponse bytesRestResponse = BytesRestResponse.createSimpleErrorResponse(
            channel,
            RestStatus.BAD_REQUEST,
            "Missing Authentication Header"
        );
        channel.sendResponse(bytesRestResponse);
        */

        // This is allowing headers without Auth header to pass through.
        // At the time of writing this, all rest-tests would fail if this is set to false
        // TODO: Change this to false once there is a decision on what to do with requests that don't have auth Headers
        return true;
    }

    /**
     * Identifies the token type and return the correct instance
     * @param authHeader from which to identify the correct token class
     * @return the instance of the token type
     */
    static AuthenticationToken tokenType(String authHeader) {
        if (authHeader.contains("Basic")) return new BasicAuthToken(authHeader);
        if (authHeader.contains("Bearer")) return new BearerAuthToken(authHeader);
        // support other type of header tokens
        return null;
    }
}
