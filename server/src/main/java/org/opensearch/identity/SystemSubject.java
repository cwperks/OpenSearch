/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.opensearch.common.annotation.InternalApi;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.util.concurrent.ThreadContextAccess;
import org.opensearch.threadpool.ThreadPool;

import java.security.Principal;
import java.util.concurrent.Callable;

/**
 * A special reserved {@link Subject} that represents the internal system subject
 *
 * @opensearch.internal
 */
@InternalApi
public class SystemSubject implements Subject {
    private static final SystemSubject INSTANCE = new SystemSubject();

    private static final SystemPrincipal SYSTEM_PRINCIPAL = new SystemPrincipal();

    private ThreadPool threadPool;

    private SystemSubject() {}

    public static SystemSubject getInstance() {
        return SystemSubject.INSTANCE;
    }

    void initialize(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public Principal getPrincipal() {
        return SYSTEM_PRINCIPAL;
    }

    @Override
    public <T> T runAs(Callable<T> callable) throws Exception {
        ThreadContext threadContext = threadPool.getThreadContext();
        try (ThreadContext.StoredContext ctx = threadPool.getThreadContext().stashContext()) {
            ThreadContextAccess.doPrivilegedVoid(threadContext::markAsSystemContext);
            return callable.call();
        }
    }

    private static class SystemPrincipal implements Principal {

        private SystemPrincipal() {}

        @Override
        public String getName() {
            return "system";
        }
    }
}
