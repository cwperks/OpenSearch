/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.spi;

import org.opensearch.cluster.metadata.Metadata;

/**
 * SPI interface for providing metadata diff capabilities for a specific category.
 * Plugins implement this to contribute their own metadata comparison logic.
 */
public interface MetadataDiffProvider {
    /** The category name (e.g., "templates", "security_roles", "ism_policies") */
    String category();

    /** Compare local and remote metadata for this category */
    CategoryDiffResult diff(Metadata local, Metadata remote);
}
