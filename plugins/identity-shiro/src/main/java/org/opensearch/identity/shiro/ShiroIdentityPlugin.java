/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.shiro;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.identity.Subject;
import org.opensearch.identity.tokens.AuthToken;
import org.opensearch.identity.tokens.RestTokenExtractor;
import org.opensearch.identity.tokens.TokenManager;
import org.opensearch.plugins.IdentityPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.tasks.Task;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Identity implementation with Shiro
 */
public final class ShiroIdentityPlugin extends Plugin implements IdentityPlugin {
    private Logger log = LogManager.getLogger(this.getClass());

    private final Settings settings;
    private final ShiroTokenManager authTokenHandler;

    /**
     * Create a new instance of the Shiro Identity Plugin
     *
     * @param settings settings being used in the configuration
     */
    public ShiroIdentityPlugin(final Settings settings) {
        this.settings = settings;
        authTokenHandler = new ShiroTokenManager();

        SecurityManager securityManager = new ShiroSecurityManager();
        SecurityUtils.setSecurityManager(securityManager);
    }

    /**
     * Return a Shiro Subject based on the provided authTokenHandler and current subject
     *
     * @return The current subject
     */
    @Override
    public Subject getSubject() {
        return new ShiroSubject(authTokenHandler, SecurityUtils.getSubject());
    }

    /**
     * Return the Shiro Token Handler
     *
     * @return the Shiro Token Handler
     */
    @Override
    public TokenManager getTokenManager() {
        return this.authTokenHandler;
    }

    @Override
    public UnaryOperator<RestHandler> authenticate(ThreadContext threadContext) {
        return AuthcRestHandler::new;
    }

    class AuthcRestHandler extends DelegatingRestHandler {
        public AuthcRestHandler(RestHandler original) {
            super(original);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
            final AuthToken token = RestTokenExtractor.extractToken(request);
            // If no token was found, continue executing the request
            if (token == null) {
                // Authentication did not fail so return true. Authorization is handled at the action level.
                delegate.handleRequest(request, channel, client);
                return;
            }
            try {
                ShiroSubject shiroSubject = (ShiroSubject) getSubject();
                shiroSubject.authenticate(token);
                // Caller was authorized, forward the request to the handler
                delegate.handleRequest(request, channel, client);
            } catch (final Exception e) {
                final BytesRestResponse bytesRestResponse = new BytesRestResponse(RestStatus.UNAUTHORIZED, e.getMessage());
                channel.sendResponse(bytesRestResponse);
            }
        }
    }
}
