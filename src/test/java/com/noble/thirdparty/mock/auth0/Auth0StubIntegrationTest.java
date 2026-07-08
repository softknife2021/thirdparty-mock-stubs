package com.noble.thirdparty.mock.auth0;

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
 * Demonstrates the full Auth0 sign-in flows against the mock:
 *  - password grant, no MFA
 *  - password grant -> mfa_required -> /mfa/challenge -> mfa-otp grant
 *  - refresh_token grant (valid + revoked)
 */
public class Auth0StubIntegrationTest extends ThirdPartyMockServerSupport {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Test
    public void signIn_noMfa_returnsTokens() throws IOException {
        String payload = "{"
                + "\"grant_type\":\"password\","
                + "\"username\":\"user@example.com\","
                + "\"password\":\"correct-horse-battery-staple\","
                + "\"client_id\":\"TEST_CLIENT_ID\","
                + "\"scope\":\"openid profile\""
                + "}";

        Response response = post("/oauth/token", payload);
        assertEquals(response.code(), 200);
        assertTrue(response.body().string().contains("\"access_token\""));
    }

    @Test
    public void signIn_withMfaEnrolledUser_requiresChallengeThenOtp() throws IOException {
        // Step 1: initial sign-in returns mfa_required + mfa_token
        String signInPayload = "{"
                + "\"grant_type\":\"password\","
                + "\"username\":\"mfa-user@example.com\","
                + "\"password\":\"correct-horse-battery-staple\","
                + "\"client_id\":\"TEST_CLIENT_ID\""
                + "}";
        Response signInResponse = post("/oauth/token", signInPayload);
        assertEquals(signInResponse.code(), 403);
        String signInBody = signInResponse.body().string();
        assertTrue(signInBody.contains("\"error\":\"mfa_required\""));

        // Step 2: request an OTP challenge with the mfa_token
        String challengePayload = "{\"mfa_token\":\"MOCK_MFA_TOKEN\",\"challenge_type\":\"otp\"}";
        Response challengeResponse = post("/mfa/challenge", challengePayload);
        assertEquals(challengeResponse.code(), 200);

        // Step 3: exchange the OTP code for tokens
        String otpPayload = "{"
                + "\"grant_type\":\"http://auth0.com/oauth/grant-type/mfa-otp\","
                + "\"mfa_token\":\"MOCK_MFA_TOKEN\","
                + "\"otp\":\"123456\","
                + "\"client_id\":\"TEST_CLIENT_ID\""
                + "}";
        Response tokenResponse = post("/oauth/token", otpPayload);
        assertEquals(tokenResponse.code(), 200);
        assertTrue(tokenResponse.body().string().contains("\"access_token\""));
    }

    @Test
    public void signIn_withMfa_wrongOtpRejected() throws IOException {
        String otpPayload = "{"
                + "\"grant_type\":\"http://auth0.com/oauth/grant-type/mfa-otp\","
                + "\"mfa_token\":\"MOCK_MFA_TOKEN\","
                + "\"otp\":\"000000\","
                + "\"client_id\":\"TEST_CLIENT_ID\""
                + "}";
        Response response = post("/oauth/token", otpPayload);
        assertEquals(response.code(), 403);
        assertTrue(response.body().string().contains("invalid_grant"));
    }

    @Test
    public void refreshToken_valid_returnsNewAccessToken() throws IOException {
        String payload = "{"
                + "\"grant_type\":\"refresh_token\","
                + "\"client_id\":\"TEST_CLIENT_ID\","
                + "\"refresh_token\":\"MOCK_REFRESH_TOKEN\""
                + "}";
        Response response = post("/oauth/token", payload);
        assertEquals(response.code(), 200);
        assertTrue(response.body().string().contains("MOCK_ACCESS_TOKEN_RENEWED"));
    }

    @Test
    public void refreshToken_revoked_returnsInvalidGrant() throws IOException {
        String payload = "{"
                + "\"grant_type\":\"refresh_token\","
                + "\"client_id\":\"TEST_CLIENT_ID\","
                + "\"refresh_token\":\"SOME_OTHER_TOKEN\""
                + "}";
        Response response = post("/oauth/token", payload);
        assertEquals(response.code(), 401);
    }

    private Response post(String path, String jsonPayload) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .post(RequestBody.create(jsonPayload, JSON))
                .build();
        return client.newCall(request).execute();
    }
}
