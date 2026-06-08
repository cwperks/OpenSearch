/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.javaagent.bootstrap;

import net.bytebuddy.asm.Advice;

/**
 * {@link Thread#stop} interceptor
 */
public class ThreadStopInterceptor {
    /**
     * ThreadStopInterceptor
     */
    public ThreadStopInterceptor() {}

    /**
     * Interceptor
     */
    @Advice.OnMethodEnter
    public static void intercept() {
        if (AgentPolicy.getPolicy() == null) {
            return; /* noop */
        }

        AgentPolicy.checkPermission(new RuntimePermission("stopThread"));
    }
}
