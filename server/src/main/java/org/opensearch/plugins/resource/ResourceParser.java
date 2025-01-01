/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource;

import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;

public interface ResourceParser<T extends Resource> {
    T parse(XContentParser xContentParser, String id) throws IOException;
}
