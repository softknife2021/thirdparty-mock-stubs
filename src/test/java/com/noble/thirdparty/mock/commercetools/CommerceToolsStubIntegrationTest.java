package com.noble.thirdparty.mock.commercetools;

import com.noble.thirdparty.mock.ThirdPartyMockServerSupport;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Demonstrates the two CommerceTools operations from the integration
 * summary: retrieve user info (GET, JWT-resolved) and create user
 * (POST, sign-up flow shared by DTC and Mobile clients).
 */
public class CommerceToolsStubIntegrationTest extends ThirdPartyMockServerSupport {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Test
    public void retrieveUserInfo_knownCustomer_returns200() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl() + "/noble-project/customers/customer-123")
                .header("Authorization", "Bearer MOCK_JWT")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 200);
            String body = response.body().string();
            assertTrue(body.contains("\"email\":\"jane.doe@example.com\""));
        }
    }

    @Test
    public void retrieveUserInfo_unknownCustomer_returns404() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl() + "/noble-project/customers/does-not-exist")
                .header("Authorization", "Bearer MOCK_JWT")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 404);
        }
    }

    @Test
    public void createUser_newEmail_returns201WithCustomerAndCart() throws IOException {
        String payload = "{"
                + "\"email\":\"new.user@example.com\","
                + "\"password\":\"correct-horse-battery-staple\","
                + "\"firstName\":\"New\","
                + "\"lastName\":\"User\""
                + "}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/noble-project/customers")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 201);
            String body = response.body().string();
            assertTrue(body.contains("\"id\":\"customer-456\""));
        }
    }

    @Test
    public void createUser_duplicateEmail_returns400() throws IOException {
        String payload = "{"
                + "\"email\":\"existing.user@example.com\","
                + "\"password\":\"correct-horse-battery-staple\""
                + "}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/noble-project/customers")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 400);
            assertTrue(response.body().string().contains("DuplicateField"));
        }
    }
}
