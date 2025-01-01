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

/**
 * Interface for a generic Resource. Resources are entities created by plugins that are typically
 * stored in system indices. Access Control is provided by the ResourceAccessControlPlugin.
 */
public interface Resource extends NamedWriteable, ToXContentObject {}
