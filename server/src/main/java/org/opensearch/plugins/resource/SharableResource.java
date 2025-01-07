/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugins.resource;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentObject;

import java.time.Instant;

/**
 * Interface for a generic Sharable Resource. Resources are entities created by plugins that are typically
 * stored in system indices. Access Control is provided by the ResourceAccessControlPlugin.
 */
public interface SharableResource extends NamedWriteable, ToXContentObject {

    /**
     * @return resource name.
     */
    String getName();

    /**
     * @return resource last update time.
     */
    Instant getLastUpdateTime();
}
