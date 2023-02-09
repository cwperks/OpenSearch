/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.rest.authinfo;

import org.opensearch.action.ActionListener;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.authn.Identity;
import org.opensearch.authn.Subject;
import org.opensearch.common.inject.Inject;
import org.opensearch.identity.User;
import org.opensearch.identity.exception.InvalidContentException;
import org.opensearch.identity.realm.InternalUsersStore;
import org.opensearch.identity.utils.ErrorType;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

/**
 * Transport action for getting information for logged in user
 */
public class TransportIdentityInfoAction extends HandledTransportAction<IdentityInfoRequest, IdentityInfoResponse> {

    @Inject
    public TransportIdentityInfoAction(TransportService transportService, ActionFilters actionFilters) {
        super(IdentityInfoAction.NAME, transportService, actionFilters, IdentityInfoRequest::new);
    }

    /**
     * Invokes 'create a user' workflow
     */
    @Override
    protected void doExecute(Task task, IdentityInfoRequest request, ActionListener<IdentityInfoResponse> listener) {
        Subject currentSubject = Identity.getAuthManager().getSubject();
        User user = null;
        if (currentSubject != null && currentSubject.getPrincipal() != null) {
            user = InternalUsersStore.getInstance().getInternalUsersModel().getUser(currentSubject.getPrincipal().getName());
        }

        if (user == null) {
            listener.onFailure(new InvalidContentException(ErrorType.SUBJECT_UNDEFINED.getMessage()));
            return;
        }

        IdentityInfoResponse response = new IdentityInfoResponse(user.toString(), user.getUsername().getName(), user.getBackendRoles());

        listener.onResponse(response);
    }

}
