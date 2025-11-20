/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.action;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.IndicesRequest;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Map;

import static org.opensearch.action.search.SearchRequest.DEFAULT_INDICES_OPTIONS;

public class WriteAdvancedSettingsRequest extends ActionRequest implements IndicesRequest.Replaceable {

    private String index;
    private Map<String, Object> settings;

    public WriteAdvancedSettingsRequest() {}

    public WriteAdvancedSettingsRequest(String index, Map<String, Object> settings) {
        this.index = index;
        this.settings = settings;
    }

    public WriteAdvancedSettingsRequest(StreamInput in) throws IOException {
        super(in);
        this.index = in.readString();
        this.settings = in.readMap();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(index);
        out.writeMap(settings);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getIndex() {
        return index;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    @Override
    public IndicesRequest indices(String... indices) {
        index = indices[0];
        return this;
    }

    @Override
    public String[] indices() {
        return new String[] { this.index };
    }

    @Override
    public IndicesOptions indicesOptions() {
        return DEFAULT_INDICES_OPTIONS;
    }
}
