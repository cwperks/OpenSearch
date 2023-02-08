/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.http;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.rest.AbstractRestChannel;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NoopResponseCollectingRestChannel implements RestChannel {
    private final AtomicInteger responses = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private List<RestResponse> capturedRestResponses = new ArrayList<>();

    private RestChannel originalChannel;

    public NoopResponseCollectingRestChannel(RestChannel originalChannel) {
        this.originalChannel = originalChannel;
    }

    @Override
    public XContentBuilder newBuilder() throws IOException {
        return null;
    }

    @Override
    public XContentBuilder newErrorBuilder() throws IOException {
        return null;
    }

    @Override
    public XContentBuilder newBuilder(XContentType xContentType, boolean useFiltering) throws IOException {
        return null;
    }

    @Override
    public XContentBuilder newBuilder(XContentType xContentType, XContentType responseContentType, boolean useFiltering) throws IOException {
        return null;
    }

    @Override
    public BytesStreamOutput bytesOutput() {
        return null;
    }

    @Override
    public RestRequest request() {
        return null;
    }

    @Override
    public boolean detailedErrorsEnabled() {
        return false;
    }

    @Override
    public void sendResponse(RestResponse response) {
        this.capturedRestResponses.add(response);
        if (response.status() == RestStatus.OK) {
            responses.incrementAndGet();
        } else {
            errors.incrementAndGet();
        }
    }

    public List<RestResponse> capturedResponses() {
        return this.capturedRestResponses;
    }

    public AtomicInteger responses() {
        return responses;
    }

    public AtomicInteger errors() {
        return errors;
    }

    public RestChannel getOriginalChannel() { return this.originalChannel; }
}

