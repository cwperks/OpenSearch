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

/**
 * Parser for Resource
 *
 * @param <T> Returns instance of a Resource
 */
public interface ResourceParser<T extends SharableResource> {
    T parse(XContentParser xContentParser, String id) throws IOException;
}
