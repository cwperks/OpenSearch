/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.realm;

import org.greenrobot.eventbus.Subscribe;
import org.opensearch.identity.configuration.model.InternalUsersModel;

/**
 * @opensearch.experimental
 */
public class InternalUsersStore {

    private static InternalUsersStore INSTANCE;

    private InternalUsersModel internalUsersModel;

    private InternalUsersStore() {}

    public static InternalUsersStore getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new InternalUsersStore();
        }

        return INSTANCE;
    }

    public InternalUsersModel getInternalUsersModel() {
        return this.internalUsersModel;
    }

    @Subscribe
    public void onInternalUsersModelChanged(InternalUsersModel ium) {
        this.internalUsersModel = ium;
    }
}
