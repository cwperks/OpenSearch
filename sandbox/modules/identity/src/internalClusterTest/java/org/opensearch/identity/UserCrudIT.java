/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity;

import org.junit.Before;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.identity.rest.IdentityRestConstants;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.util.Map;

import static org.opensearch.test.rest.OpenSearchRestTestCase.entityAsMap;

/**
 * Tests REST API for users against local cluster
 */
@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 0)
public class UserCrudIT extends HttpSmokeTestCaseWithIdentity {
    private static final String ENDPOINT = IdentityRestConstants.IDENTITY_REST_API_REQUEST_PREFIX;;

    public UserCrudIT() {}

    @Before
    public void startClusterWithIdentityIndex() throws Exception {
        startNodesWithIdentityIndex();
    }

    @SuppressWarnings("unchecked")
    public void testUsersRestApi() throws Exception {

        String username = "test-create";
        String requestContent = "{ \"password\" : \"test\","
            + " \"attributes\": { \"attribute1\": \"value1\"},"
            + " \"permissions\": [\"indices:admin:create\"]"
            + " }\n";

        // Create a user
        String createSuccessMessage = username + " created successfully.";
        Request createRequest = new Request("PUT", ENDPOINT + "/users/" + username);
        createRequest.setJsonEntity(requestContent);
        RequestOptions options = RequestOptions.DEFAULT.toBuilder().addHeader("Authorization", "Basic YWRtaW46YWRtaW4=").build(); // admin:admin
        createRequest.setOptions(options);
        Response response = getRestClient().performRequest(createRequest);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        Map<String, Object> userCreated = entityAsMap(response);
        assertEquals(userCreated.size(), 3);
        assertEquals(userCreated.get("successful"), true);
        assertEquals(userCreated.get("username"), username);
        assertEquals(userCreated.get("message"), createSuccessMessage);

        // Update a user (same user in this case)
        String updateSuccessMessage = username + " updated successfully.";
        Request updateRequest = new Request("PUT", ENDPOINT + "/users/" + username);
        updateRequest.setJsonEntity(requestContent);
        updateRequest.setOptions(options);
        response = getRestClient().performRequest(updateRequest);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        Map<String, Object> usersUpdated = entityAsMap(response);
        assertEquals(usersUpdated.size(), 3);
        assertEquals(usersUpdated.get("successful"), true);
        assertEquals(usersUpdated.get("username"), username);
        assertEquals(usersUpdated.get("message"), updateSuccessMessage);

        // TODO: Add other api tests here
    }

}
