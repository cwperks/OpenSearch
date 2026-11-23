/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.rest;

import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.ActionModule;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.ActionType;
import org.opensearch.action.admin.cluster.storedscripts.GetScriptContextAction;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.action.support.ActionFilterChain;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.ActionRequestMetadata;
import org.opensearch.action.support.TransportAction;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.action.admin.cluster.RestGetScriptContextAction;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskManager;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.rest.FakeRestChannel;
import org.opensearch.test.rest.FakeRestRequest;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.node.NodeClient;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaseRestActionHandlerTests extends OpenSearchTestCase {
    private static final ActionType<TestResponse> ACTION = new ActionType<>("cluster:monitor/example", in -> new TestResponse());

    public void testDispatchPreservesContextAndRunsFilters() throws Exception {
        assertDispatch(true);
    }

    public void testDeniedActionDoesNotExecute() throws Exception {
        assertDispatch(false);
    }

    private void assertDispatch(boolean allowed) throws Exception {
        ThreadPool threadPool = new TestThreadPool(getTestName());
        AtomicBoolean filtered = new AtomicBoolean();
        AtomicBoolean executed = new AtomicBoolean();
        try (NodeClient client = new NodeClient(Settings.EMPTY, threadPool)) {
            threadPool.getThreadContext().putTransient("caller", "restricted-user");
            threadPool.getThreadContext().putHeader("request-marker", "original");
            ActionFilter filter = new ActionFilter() {
                @Override
                public int order() {
                    return 0;
                }

                @Override
                public <Request extends ActionRequest, Response extends ActionResponse> void apply(
                    Task task,
                    String action,
                    Request request,
                    ActionRequestMetadata<Request, Response> metadata,
                    ActionListener<Response> listener,
                    ActionFilterChain<Request, Response> chain
                ) {
                    assertEquals(ACTION.name(), action);
                    assertEquals("restricted-user", threadPool.getThreadContext().getTransient("caller"));
                    assertEquals("original", threadPool.getThreadContext().getHeader("request-marker"));
                    filtered.set(true);
                    if (allowed) {
                        chain.proceed(task, action, request, listener);
                    } else {
                        listener.onFailure(new OpenSearchStatusException("denied", RestStatus.FORBIDDEN));
                    }
                }
            };
            TransportAction<TestRequest, TestResponse> transportAction = new TransportAction<>(
                ACTION.name(),
                new ActionFilters(Set.of(filter)),
                new TaskManager(Settings.EMPTY, threadPool, Set.of())
            ) {
                @Override
                protected void doExecute(Task task, TestRequest request, ActionListener<TestResponse> listener) {
                    executed.set(true);
                    listener.onResponse(new TestResponse());
                }
            };
            ActionModule.DynamicActionRegistry registry = new ActionModule.DynamicActionRegistry();
            registry.registerUnmodifiableActionMap(Map.of(ACTION, transportAction));
            client.initialize(registry, () -> "local", null, null);
            TestHandler handler = new TestHandler();
            FakeRestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withPath("/example").build();
            FakeRestChannel channel = new FakeRestChannel(request, true, 1);
            handler.handleRequest(request, channel, client);
            assertSame(ACTION, handler.action());
            assertTrue(filtered.get());
            assertEquals(allowed, executed.get());
            assertEquals(allowed ? RestStatus.OK : RestStatus.FORBIDDEN, channel.capturedResponse().status());
            assertEquals("restricted-user", threadPool.getThreadContext().getTransient("caller"));
        } finally {
            ThreadPool.terminate(threadPool, 10, TimeUnit.SECONDS);
        }
    }

    public void testUnconsumedParametersPreventDispatch() throws Exception {
        TestHandler handler = new TestHandler();
        FakeRestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withPath("/example")
            .withParams(Map.of("unsupported", "value"))
            .build();
        // Dispatch would dereference the null client if parameter validation did not run first.
        expectThrows(IllegalArgumentException.class, () -> handler.handleRequest(request, new FakeRestChannel(request, true, 1), null));
    }

    public void testScriptContextDeclaresItsActionAndRoute() {
        RestGetScriptContextAction handler = new RestGetScriptContextAction();
        assertSame(GetScriptContextAction.INSTANCE, handler.action());
        assertEquals(1, handler.routes().size());
        assertEquals(RestRequest.Method.GET, handler.routes().get(0).getMethod());
        assertEquals("/_script_context", handler.routes().get(0).getPath());
    }

    private static class TestHandler extends BaseRestActionHandler<TestRequest, TestResponse> {
        TestHandler() {
            super(ACTION, new Route(RestRequest.Method.GET, "/example"));
        }

        @Override
        public String getName() {
            return "example";
        }

        @Override
        protected TestRequest parseRequest(RestRequest request) {
            return new TestRequest();
        }
    }

    private static class TestRequest extends ActionRequest {
        @Override
        public ActionRequestValidationException validate() {
            return null;
        }
    }

    private static class TestResponse extends ActionResponse implements ToXContentObject {
        @Override
        public void writeTo(StreamOutput out) {}

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.startObject().field("ok", true).endObject();
        }
    }
}
