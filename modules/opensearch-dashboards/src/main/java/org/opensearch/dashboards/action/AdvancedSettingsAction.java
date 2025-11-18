/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.dashboards.action;

import org.opensearch.action.ActionType;

public class AdvancedSettingsAction extends ActionType<AdvancedSettingsResponse> {

    public static final AdvancedSettingsAction INSTANCE = new AdvancedSettingsAction();
    public static final String NAME = "osd:admin/advanced_settings";

    private AdvancedSettingsAction() {
        super(NAME, AdvancedSettingsResponse::new);
    }
}
