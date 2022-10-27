package io.quarkus.ts.security.keycloak.oidcclient.extended.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.security.keycloak.oidcclient.extended.resources.OidcWiremockTestResource;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.tokens.LogoutFlow;
import io.quarkus.ts.security.keycloak.oidcclient.extended.restclient.tokens.LogoutTenantResolver;

@QuarkusScenario
public class LogoutFlowIT {

    private static OidcWiremockTestResource keycloakServer;

    @QuarkusApplication(classes = { LogoutFlow.class, LogoutTenantResolver.class })
    static RestService app = new RestService()
            .onPreStart(a -> onStart())
            .withProperty("keycloak.url", () -> keycloakServer.getWireMockServer().baseUrl() + "/auth")
            .withProperties("logout.properties");

    @AfterAll
    public static void tearDown() {
        keycloakServer.stop();
    }

    @Test
    public void testCodeFlow() throws IOException {
        defineCodeFlowLogoutStub();
        try (final WebClient webClient = createWebClient()) {
            webClient.getOptions().setRedirectEnabled(true);
            HtmlPage page = webClient.getPage("http://localhost:1101/code-flow");

            HtmlForm form = page.getFormByName("form");
            form.getInputByName("username").type("alice");
            form.getInputByName("password").type("alice");

            page = form.getInputByValue("login").click();

            assertEquals("alice, cache size: 0", page.getBody().asNormalizedText());
            assertNotNull(getSessionCookie(webClient, "code-flow"));

            page = webClient.getPage("http://localhost:1101/code-flow/logout");
            assertEquals("Welcome, clientId: quarkus-web-app", page.getBody().asNormalizedText());
            assertNull(getSessionCookie(webClient, "code-flow"));
            // Clear the post logout cookie
            webClient.getCookieManager().clearCookies();
        }
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }

    private void defineCodeFlowLogoutStub() {
        keycloakServer.getWireMockServer().stubFor(
                get(urlPathMatching("/auth/realms/quarkus/protocol/openid-connect/end-session"))
                        .willReturn(aResponse()
                                .withHeader("Location",
                                        "{{request.query.returnTo}}?clientId={{request.query.client_id}}")
                                .withStatus(302)
                                .withTransformers("response-template")));
    }

    private Cookie getSessionCookie(WebClient webClient, String tenantId) {
        return webClient.getCookieManager().getCookie("q_session" + (tenantId == null ? "" : "_" + tenantId));
    }

    private static void onStart() {
        keycloakServer = new OidcWiremockTestResource();
        keycloakServer.start(false);
    }
}
