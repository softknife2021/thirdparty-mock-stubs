package com.noble.thirdparty.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

/**
 * Shared base class for TestNG tests that need a running third-party mock
 * server. One WireMock instance serves every provider's stub mappings
 * (Braze, Auth0, CommerceTools, ...) since each provider uses distinct
 * URL paths - no need for one server per provider.
 *
 * <p>Mappings live under {@code src/test/resources/wiremock/mappings},
 * one JSON file per stubbed endpoint, named
 * {@code <provider>-<operation>.json} (e.g. {@code auth0-token-refresh.json}).</p>
 *
 * <p>Started once per suite (not per class) so multiple provider test
 * classes in the same run share one instance and one port.</p>
 */
public abstract class ThirdPartyMockServerSupport {

    protected static WireMockServer wireMockServer;
    private static final int PORT = 8443;

    @BeforeSuite(alwaysRun = true)
    public void startMockServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            return;
        }
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .port(PORT)
                        .usingFilesUnderDirectory("src/test/resources/wiremock")
        );
        wireMockServer.start();
        configureFor("localhost", PORT);
    }

    @AfterSuite(alwaysRun = true)
    public void stopMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    protected static String baseUrl() {
        return wireMockServer.baseUrl();
    }
}
