/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;

public class SystemSubjectTests extends OpenSearchTestCase {
    public void testSystemSubject() {
        ThreadPool threadPool = new TestThreadPool(getTestName());
        SystemSubject systemSubject = SystemSubject.getInstance();
        assertEquals("system", systemSubject.getPrincipal().getName());
        systemSubject.initialize(threadPool);
        assertFalse(threadPool.getThreadContext().isSystemContext());
        systemSubject.runAs(() -> {
            assertTrue(threadPool.getThreadContext().isSystemContext());
            return false;
        });
        terminate(threadPool);
    }
}
