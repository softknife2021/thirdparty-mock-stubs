# Third-Party Mock Stubs (WireMock + TestNG + Gradle)

Covers Braze, Auth0, and CommerceTools so far. One shared WireMock instance
serves every provider (see "Project layout" below) since each provider uses
distinct URL paths.

## Where the React Native SDK actually calls Braze

Short answer: **it doesn't, directly.** `@braze/react-native-sdk` (the
`braze-react-native-sdk` GitHub repo) is a thin JS bridge. Calling
`Braze.logCustomEvent()`, `Braze.changeUser()`, etc. from JS just invokes a
native module method over the RN bridge/Turbo Module. All actual networking
happens inside the **native** SDKs that RN wraps:

- **Android**: `braze-android-sdk` (Kotlin/Java, closed network stack built
  on OkHttp internally). Source is on GitHub but the dispatch/network layer
  ships mostly as compiled artifacts.
- **iOS**: `braze-swift-sdk`.

So there's no single line of JS you can point to — the call happens after
the bridge hands off to native code, on a background dispatch thread that
batches events/attributes/purchases and flushes them on a timer (or via
`requestImmediateDataFlush()`).

### The two endpoints that matter

| Endpoint | Who calls it | Purpose |
|---|---|---|
| `POST https://sdk.<cluster>.braze.com/api/v3/data` | The **native SDK** (Android/iOS), triggered indirectly by RN JS calls | Sessions, custom events, attributes, purchases — this is what "pairing sensor" / "device foreground/background" map to |
| `POST https://rest.<cluster>.braze.com/users/track` (+ `/identify`, `/delete`) | **Your backend**, server-to-server | Historical import, account linking, GDPR deletes — not called by the mobile client |

`<cluster>` is account-specific (e.g. `sdk.iad-01.braze.com`). Find yours in
the Braze dashboard under Manage Settings, or via
`res/values/braze.xml` (`com_braze_custom_endpoint`) in an Android checkout.

This project stubs **both** endpoints so you can test whichever layer you own.

## Project layout

```
braze-mock-stubs/
├── build.gradle
├── settings.gradle
├── src/test/java/com/noble/thirdparty/mock/
│   ├── ThirdPartyMockServerSupport.java   # starts/stops ONE shared WireMock instance per suite
│   ├── braze/BrazeStubIntegrationTest.java
│   ├── auth0/Auth0StubIntegrationTest.java
│   └── commercetools/CommerceToolsStubIntegrationTest.java
└── src/test/resources/
    ├── testng.xml
    └── wiremock/mappings/
        ├── braze-sdk-data.json                     # POST /api/v3/data (happy path)
        ├── braze-sdk-data-unauthorized.json         # POST /api/v3/data (invalid key)
        ├── braze-users-track.json                  # POST /users/track
        ├── braze-users-identify.json               # POST /users/identify
        ├── braze-users-delete.json                 # POST /users/delete
        ├── auth0-token-signin-success.json          # POST /oauth/token (password, no MFA)
        ├── auth0-token-signin-mfa-required.json     # POST /oauth/token -> 403 mfa_required
        ├── auth0-mfa-challenge.json                 # POST /mfa/challenge
        ├── auth0-token-mfa-otp-success.json         # POST /oauth/token (mfa-otp, correct code)
        ├── auth0-token-mfa-otp-invalid.json         # POST /oauth/token (mfa-otp, wrong code)
        ├── auth0-token-refresh-success.json         # POST /oauth/token (refresh_token, valid)
        ├── auth0-token-refresh-invalid.json         # POST /oauth/token (refresh_token, revoked)
        ├── commercetools-get-customer-success.json  # GET /{projectKey}/customers/{id}
        ├── commercetools-get-customer-not-found.json
        ├── commercetools-create-customer-success.json   # POST /{projectKey}/customers
        └── commercetools-create-customer-duplicate.json
```

## Running

```bash
./gradlew test
```

## Using it

1. **Testing backend code that calls the Braze REST API**: point your HTTP
   client's base URL at `wireMockServer.baseUrl()` (or run WireMock
   standalone and point your Spring Boot config's `braze.rest-endpoint` at
   `http://localhost:8443` for an integration test / `@SpringBootTest`).
2. **Testing native Android code**: run WireMock standalone
   (`java -jar wiremock-standalone.jar --port 8443 --root-dir wiremock/`),
   then set `com_braze_custom_endpoint` to your machine's IP:port (or use
   `adb reverse tcp:8443 tcp:8443` for an emulator) so the real Android SDK
   dispatches to the mock instead of Braze's servers.
3. **Extending stubs**: add new files under
   `src/test/resources/wiremock/mappings/`. Each is a standard WireMock
   mapping — match on `urlPath`, `method`, and `bodyPatterns` (JSONPath),
   respond with `jsonBody`.

## Auth0 flow covered

`password grant → (optional) mfa_required → /mfa/challenge → mfa-otp grant → tokens`,
plus `refresh_token` grant (valid and revoked). See `Auth0StubIntegrationTest`
for the full sequence including the MFA step-up.

## CommerceTools flow covered

`GET /{projectKey}/customers/{id}` (found + 404) and
`POST /{projectKey}/customers` (sign-up success + duplicate-email 400).
Project key is hardcoded to `noble-project` in the mappings — change the
`urlPath`/`urlPathPattern` values if yours differs.

## Extending to other providers

The same pattern (WireMock mappings under one shared directory + a
provider subpackage + TestNG tests extending `ThirdPartyMockServerSupport`)
works for the rest of the integration summary — OneTrust consent receipts,
Harness feature flags, Sentry error events, Amplitude events, FatSecret,
Voucherify. Happy to generate any of those next.
# thirdparty-mock-stubs
