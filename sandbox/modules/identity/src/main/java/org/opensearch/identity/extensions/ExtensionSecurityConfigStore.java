/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.extensions;

import org.greenrobot.eventbus.Subscribe;

/**
 * @opensearch.experimental
 */
public class ExtensionSecurityConfigStore {

    private static ExtensionSecurityConfigStore INSTANCE;

    private ExtensionSecurityConfigModel extensionsSecurityConfigModel;

    private ExtensionSecurityConfigStore() {}

    public static ExtensionSecurityConfigStore getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExtensionSecurityConfigStore();
        }

        return INSTANCE;
    }

    ExtensionSecurityConfigModel getExtensionsSecurityConfigModel() {
        return this.extensionsSecurityConfigModel;
    }

    @Subscribe
    public void onExtensionsSecurityConfigModelChanged(ExtensionSecurityConfigModel esm) {
        this.extensionsSecurityConfigModel = esm;
    }
}

