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
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Map;

public class AdvancedSettingsRequest extends ActionRequest {

    private String index;
    private Map<String, Object> settings;

    public AdvancedSettingsRequest() {}

    public AdvancedSettingsRequest(String index, Map<String, Object> settings) {
        this.index = index;
        this.settings = settings;
    }

    public AdvancedSettingsRequest(StreamInput in) throws IOException {
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
}
