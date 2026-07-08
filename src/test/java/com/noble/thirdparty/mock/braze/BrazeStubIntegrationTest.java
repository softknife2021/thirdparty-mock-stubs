package com.noble.thirdparty.mock.braze;

import com.noble.thirdparty.mock.ThirdPartyMockServerSupport;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Demonstrates hitting the mocked Braze endpoints the same way the native
 * Braze SDK (behind the RN bridge) or a backend service would.
 *
 * These are stub-level tests (fast, no network to real Braze). Use them to
 * verify your own client/dispatch code builds the right payload and handles
 * the response correctly, before wiring up a real Braze sandbox key.
 */
public class BrazeStubIntegrationTest extends ThirdPartyMockServerSupport {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Test
    public void sdkDataEndpoint_acceptsSessionAndEvents() throws IOException {
        String payload = "{"
                + "\"api_key\":\"TEST_API_KEY\","
                + "\"device_id\":\"device-123\","
                + "\"sdk_version\":\"39.0.0\","
                + "\"events\":[{\"name\":\"button_clicked\",\"time\":1751800000}],"
                + "\"attributes\":[{\"external_id\":\"user-123\",\"first_name\":\"Jane\"}]"
                + "}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/api/v3/data")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 200);
            String body = response.body().string();
            assertTrue(body.contains("\"success\":true"));
        }

        verify(postRequestedFor(urlPathEqualTo("/api/v3/data")));
    }

    @Test
    public void sdkDataEndpoint_rejectsInvalidApiKey() throws IOException {
        String payload = "{\"api_key\":\"INVALID_KEY\",\"device_id\":\"device-123\"}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/api/v3/data")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 401);
        }
    }

    @Test
    public void restApi_usersTrack_returnsSuccess() throws IOException {
        String payload = "{"
                + "\"attributes\":[{\"external_id\":\"user-123\",\"email\":\"[email protected]\"}]"
                + "}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/users/track")
                .header("Authorization", "Bearer TEST_REST_API_KEY")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 201);
            assertTrue(response.body().string().contains("\"message\":\"success\""));
        }
    }

    @Test
    public void restApi_usersIdentify_returnsSuccess() throws IOException {
        String payload = "{"
                + "\"aliases_to_identify\":[{"
                + "\"external_id\":\"user-123\","
                + "\"user_alias\":{\"alias_name\":\"anon-456\",\"alias_label\":\"device_id\"}"
                + "}]}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/users/identify")
                .header("Authorization", "Bearer TEST_REST_API_KEY")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 201);
        }
    }

    @Test
    public void restApi_usersDelete_returnsSuccess() throws IOException {
        String payload = "{\"external_ids\":[\"user-123\"]}";

        Request request = new Request.Builder()
                .url(baseUrl() + "/users/delete")
                .header("Authorization", "Bearer TEST_REST_API_KEY")
                .post(RequestBody.create(payload, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(response.code(), 201);
        }
    }
}
