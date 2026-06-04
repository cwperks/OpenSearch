/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.remotecluster.spi;

/**
 * Extension interface that plugins implement to register metadata diff providers.
 * Plugins that want to contribute metadata categories to the diff API should
 * implement this interface in their Plugin class.
 */
public interface MetadataDiffExtension {
    /** Return the metadata diff provider for this plugin's category */
    MetadataDiffProvider getMetadataDiffProvider();
}
