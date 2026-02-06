/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.common.xcontent;

import com.fasterxml.jackson.core.StreamReadConstraints;

import org.opensearch.common.annotation.InternalApi;

/**
 * Consolidates the XContent constraints (primarily reflecting Jackson's {@link StreamReadConstraints} constraints)
 *
 * @opensearch.internal
 */
@InternalApi
public interface XContentConstraints {
    String DEFAULT_CODEPOINT_LIMIT_PROPERTY = "opensearch.xcontent.codepoint.max";
    String DEFAULT_MAX_STRING_LEN_PROPERTY = "opensearch.xcontent.string.length.max";
    String DEFAULT_MAX_NAME_LEN_PROPERTY = "opensearch.xcontent.name.length.max";
    String DEFAULT_MAX_DEPTH_PROPERTY = "opensearch.xcontent.depth.max";

    int DEFAULT_MAX_STRING_LEN = Integer.parseInt(System.getProperty(DEFAULT_MAX_STRING_LEN_PROPERTY, "50000000" /* ~50 Mb */));

    int DEFAULT_MAX_NAME_LEN = Integer.parseInt(
        System.getProperty(DEFAULT_MAX_NAME_LEN_PROPERTY, "50000" /* StreamReadConstraints.DEFAULT_MAX_NAME_LEN */)
    );

    int DEFAULT_MAX_DEPTH = Integer.parseInt(
        System.getProperty(DEFAULT_MAX_DEPTH_PROPERTY, "1000" /* StreamReadConstraints.DEFAULT_MAX_DEPTH */)
    );

    int DEFAULT_CODEPOINT_LIMIT = Integer.parseInt(System.getProperty(DEFAULT_CODEPOINT_LIMIT_PROPERTY, "52428800" /* ~50 Mb */));
}
