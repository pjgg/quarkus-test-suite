package io.quarkus.ts.http.advanced.reactive;

import static com.gargoylesoftware.htmlunit.util.MimeType.IMAGE_JPEG;
import static com.gargoylesoftware.htmlunit.util.MimeType.IMAGE_PNG;
import static com.gargoylesoftware.htmlunit.util.MimeType.TEXT_CSS;
import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.ANY_ENCODING;
import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.APPLICATION_YAML;
import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.ENGLISH;
import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.JAPANESE;
import static io.quarkus.ts.http.advanced.reactive.MediaTypeResource.MEDIA_TYPE_PATH;
import static io.quarkus.ts.http.advanced.reactive.MultipartResource.FILE;
import static io.quarkus.ts.http.advanced.reactive.MultipartResource.MULTIPART_FORM_PATH;
import static io.quarkus.ts.http.advanced.reactive.MultipartResource.TEXT;
import static io.quarkus.ts.http.advanced.reactive.MultipleResponseSerializersResource.APPLY_RESPONSE_SERIALIZER_PARAM_FLAG;
import static io.quarkus.ts.http.advanced.reactive.MultipleResponseSerializersResource.MULTIPLE_RESPONSE_SERIALIZERS_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.apache.http.HttpHeaders.ACCEPT_ENCODING;
import static org.apache.http.HttpHeaders.ACCEPT_LANGUAGE;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusVersion;
import io.quarkus.test.scenarios.annotations.EnabledOnQuarkusVersion;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.http.advanced.reactive.clients.HealthClientService;
import io.quarkus.ts.http.advanced.reactive.clients.HttpVersionClientService;
import io.quarkus.ts.http.advanced.reactive.clients.HttpVersionClientServiceAsync;
import io.quarkus.ts.http.advanced.reactive.clients.RestClientServiceBuilder;
import io.restassured.http.Header;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicate;
import io.vertx.mutiny.ext.web.client.predicate.ResponsePredicateResult;

@Tag("fips-incompatible") // native-mode
@QuarkusScenario
public class HttpAdvancedReactiveIT {

    private static final String REALM_DEFAULT = "test-realm";
    private static final String ROOT_PATH = "/api";
    private static final String HELLO_ENDPOINT = ROOT_PATH + "/hello";
    private static final int TIMEOUT_SEC = 3;
    private static final int RETRY = 3;
    private static final String PASSWORD = "password";
    private static final String KEY_STORE_PATH = "META-INF/resources/server.keystore";
    private static final int KEYCLOAK_PORT = 8080;
    private static final int ASSERT_TIMEOUT_SECONDS = 10;
    private static final String UTF_8_CHARSET = ";charset=UTF-8";
    private static final String CONTENT = "content";

    //TODO Remove workaround after Keycloak is fixed https://github.com/keycloak/keycloak/issues/9916
    @Container(image = "${keycloak.image}", expectedLog = "Admin console listening", port = KEYCLOAK_PORT)
    static KeycloakService keycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT)
            .withProperty("JAVA_OPTS", "-Dcom.redhat.fips=false");

    @QuarkusApplication(ssl = true)
    static RestService app = new RestService().withProperty("quarkus.oidc.auth-server-url",
            keycloak::getRealmUrl);

    @Test
    @DisplayName("Http/1.1 Server test")
    public void httpServer() {
        app.given().get(HELLO_ENDPOINT)
                .then().statusLine("HTTP/1.1 200 OK").statusCode(SC_OK)
                .body("content", is("Hello, World!"));
    }

    @Test
    @DisplayName("GRPC Server test")
    public void testGrpc() {
        app.given().when().get("/api/grpc/trinity").then().statusCode(SC_OK).body(is("Hello trinity"));
    }

    @Test
    @DisplayName("Http/2 Server test")
    public void http2Server() throws InterruptedException, URISyntaxException {
        CountDownLatch done = new CountDownLatch(1);
        Uni<JsonObject> content = app.mutiny(defaultVertxHttpClientOptions())
                .getAbs(getAppEndpoint() + "/hello")
                .expect(ResponsePredicate.create(this::isHttp2x))
                .expect(ResponsePredicate.status(Response.Status.OK.getStatusCode())).send()
                .map(HttpResponse::bodyAsJsonObject).ifNoItem().after(Duration.ofSeconds(TIMEOUT_SEC)).fail()
                .onFailure().retry().atMost(RETRY);

        content.subscribe().with(body -> {
            assertEquals(body.getString("content"), "Hello, World!");
            done.countDown();
        });

        done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertThat(done.getCount(), equalTo(0L));
    }

    @Test
    @DisplayName("Http/2 Client Sync test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientSync() throws Exception {
        HttpVersionClientService versionHttpClient = new RestClientServiceBuilder<HttpVersionClientService>(
                getAppEndpoint()).withHostVerified(true).withPassword(PASSWORD).withKeyStorePath(KEY_STORE_PATH)
                        .build(HttpVersionClientService.class);

        Response resp = versionHttpClient.getClientHttpVersion();
        assertEquals(SC_OK, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HttpClientVersionResource.HTTP_VERSION));
    }

    @Test
    @DisplayName("Http/2 Client Async test")
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-658")
    public void http2ClientAsync() throws Exception {
        HttpVersionClientServiceAsync clientServiceAsync = new RestClientServiceBuilder<HttpVersionClientServiceAsync>(
                getAppEndpoint()).withHostVerified(true).withPassword(PASSWORD).withKeyStorePath(KEY_STORE_PATH)
                        .build(HttpVersionClientServiceAsync.class);

        Response resp = clientServiceAsync.getClientHttpVersion().await().atMost(Duration.ofSeconds(ASSERT_TIMEOUT_SECONDS));

        assertEquals(SC_OK, resp.getStatus());
        assertEquals(HttpVersion.HTTP_2.name(), resp.getHeaderString(HttpClientVersionResource.HTTP_VERSION));
    }

    @Test
    @DisplayName("Non-application endpoint move to /q/")
    @EnabledOnQuarkusVersion(version = "1\\..*", reason = "Redirection is no longer supported in 2.x")
    public void nonAppRedirections() {
        List<String> endpoints = Arrays.asList("/openapi", "/swagger-ui", "/metrics/base", "/metrics/application",
                "/metrics/vendor", "/metrics", "/health/group", "/health/well", "/health/ready", "/health/live",
                "/health");

        for (String endpoint : endpoints) {
            app.given().redirects().follow(false).get(ROOT_PATH + endpoint)
                    .then().statusCode(HttpStatus.SC_MOVED_PERMANENTLY)
                    .and().header("Location", containsString("/q" + endpoint));

            app.given().get(ROOT_PATH + endpoint)
                    .then().statusCode(in(Arrays.asList(SC_OK, HttpStatus.SC_NO_CONTENT)));
        }
    }

    @Test
    @Disabled("blocked by: https://issues.redhat.com/browse/QUARKUS-781")
    public void microprofileHttpClientRedirection() throws Exception {
        HealthClientService healthHttpClient = new RestClientServiceBuilder<HealthClientService>(getAppEndpoint())
                .withHostVerified(true).withPassword(PASSWORD).withKeyStorePath(KEY_STORE_PATH)
                .build(HealthClientService.class);

        assertThat(SC_OK, equalTo(healthHttpClient.health().getStatus()));
    }

    @Test
    @EnabledOnQuarkusVersion(version = "1\\..*", reason = "Redirection is no longer supported in 2.x")
    public void vertxHttpClientRedirection() throws InterruptedException, URISyntaxException {
        CountDownLatch done = new CountDownLatch(1);
        Uni<Integer> statusCode = app.mutiny(defaultVertxHttpClientOptions())
                .getAbs(getAppEndpoint() + "/health").send()
                .map(HttpResponse::statusCode).ifNoItem()
                .after(Duration.ofSeconds(TIMEOUT_SEC)).fail().onFailure().retry().atMost(RETRY);

        statusCode.subscribe().with(httpStatusCode -> {
            assertEquals(SC_OK, httpStatusCode);
            done.countDown();
        });

        done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertThat(done.getCount(), equalTo(0L));
    }

    @DisplayName("RESTEasy Reactive Multipart Provider test")
    @Test
    public void multipartFormDataReader() {
        app.given()
                .multiPart(FILE, Paths.get("src", "test", "resources", "file.txt").toFile())
                .formParam(TEXT, TEXT)
                .post(ROOT_PATH + MULTIPART_FORM_PATH)
                .then().statusCode(SC_OK)
                .body(FILE, is("File content"))
                .body(TEXT, is(TEXT));
    }

    @DisabledOnQuarkusVersion(version = "(2\\.[0-8]\\..*)|(2\\.9\\.[0-1]\\..*)", reason = "Fixed in Quarkus 2.9.2.Final")
    @DisplayName("JAX-RS RouterFilter and Vert.x Web Routes integration")
    @Test
    public void multipleResponseFilter() {
        // test headers from both filters are present, that is useful content negotiation
        // scenario -> server side should be able to set multiple VARY response headers
        // so browser can identify when to serve cached response and when to send request to a server side
        var headers = app.given().get(ROOT_PATH + MEDIA_TYPE_PATH).headers().asList();
        assertHasHeaderWithValue(HttpHeaders.ACCEPT_LANGUAGE, ENGLISH, headers);
        assertHasHeaderWithValue(HttpHeaders.ACCEPT_LANGUAGE, JAPANESE, headers);
        assertHasHeaderWithValue(HttpHeaders.ACCEPT_ENCODING, ANY_ENCODING, headers);
        assertHasHeaderWithValue(HttpHeaders.VARY, ACCEPT_ENCODING, headers);
        assertHasHeaderWithValue(HttpHeaders.VARY, ACCEPT_LANGUAGE, headers);
    }

    @DisplayName("Several Resources share same base path test")
    @Test
    public void severalResourcesSameBasePath() {
        // following endpoints are placed in 2 different Resources with the same base path
        app.given().get(HELLO_ENDPOINT).then().body(CONTENT, is("Hello, World!"));
        app.given().get(HELLO_ENDPOINT + HelloAllResource.ALL_ENDPOINT_PATH).then().body(CONTENT, is("Hello all, World!"));
    }

    private void assertHasHeaderWithValue(String headerName, String headerValue, List<Header> headers) {
        Assertions.assertTrue(
                headers
                        .stream()
                        .filter(h -> h.getName().equals(headerName))
                        .map(Header::getValue)
                        .anyMatch(headerValue::equals));
    }

    @DisabledOnQuarkusVersion(version = "(2\\.[0-6]\\..*)|(2\\.7\\.[0-5]\\..*)|(2\\.8\\.[0-2]\\..*)", reason = "Fixed in Quarkus 2.8.3.Final. and backported to 2.7.6.Final")
    @DisplayName("JAX-RS MessageBodyWriter test")
    @Test
    public void messageBodyWriter() {
        // test MediaType is passed to MessageBodyWriter correctly
        String mediaTypeProperty = "mediaType";
        app
                .given()
                .get(ROOT_PATH + MEDIA_TYPE_PATH)
                .then()
                .statusCode(SC_OK)
                .body(mediaTypeProperty, notNullValue())
                .body(mediaTypeProperty + ".type", is("application"))
                .body(mediaTypeProperty + ".subtype", is("json"));
    }

    @DisabledOnQuarkusVersion(version = "(2\\.[0-8]\\..*)|(2\\.9\\.0\\..*)", reason = "Fixed in Quarkus 2.9.1.Final")
    @DisplayName("JAX-RS Response Content type test")
    @Test
    public void responseContentType() {
        testResponseContentType(APPLICATION_JSON, APPLICATION_JSON + UTF_8_CHARSET);
        testResponseContentType(APPLICATION_XML, APPLICATION_XML + UTF_8_CHARSET);
        testResponseContentType(APPLICATION_YAML, APPLICATION_YAML + UTF_8_CHARSET);
        testResponseContentType(TEXT_HTML, TEXT_HTML + UTF_8_CHARSET);
        testResponseContentType(TEXT_PLAIN, TEXT_PLAIN + UTF_8_CHARSET);
        testResponseContentType(TEXT_CSS, TEXT_CSS + UTF_8_CHARSET);
        testResponseContentType(TEXT_XML, TEXT_XML + UTF_8_CHARSET);
        testResponseContentType(APPLICATION_OCTET_STREAM, APPLICATION_OCTET_STREAM);
        testResponseContentType(MULTIPART_FORM_DATA, MULTIPART_FORM_DATA);
        testResponseContentType(IMAGE_PNG, IMAGE_PNG);
        testResponseContentType(IMAGE_JPEG, IMAGE_JPEG);
    }

    @DisabledOnQuarkusVersion(version = "(2\\.[0-6]\\..*)|(2\\.7\\.[0-5]\\..*)|(2\\.8\\.0\\..*)", reason = "Fixed in Quarkus 2.8.1 and backported to 2.7.6.")
    @Test
    public void testMediaTypePassedToMessageBodyWriter() {
        // Accepted Media Type must be passed to 'MessageBodyWriter'
        // 'MessageBodyWriter' then returns passed Media Type for a verification
        assertAcceptedMediaTypeEqualsResponseBody(APPLICATION_JSON);
        assertAcceptedMediaTypeEqualsResponseBody(TEXT_HTML);
        assertAcceptedMediaTypeEqualsResponseBody(TEXT_PLAIN);
        assertAcceptedMediaTypeEqualsResponseBody(APPLICATION_OCTET_STREAM);
    }

    private void assertAcceptedMediaTypeEqualsResponseBody(String acceptedMediaType) {
        app
                .given()
                .accept(acceptedMediaType)
                .queryParam(APPLY_RESPONSE_SERIALIZER_PARAM_FLAG, Boolean.TRUE)
                .get(ROOT_PATH + MULTIPLE_RESPONSE_SERIALIZERS_PATH)
                .then()
                .body(is(acceptedMediaType));
    }

    private void testResponseContentType(String acceptedContentType, String expectedContentType) {
        app.given()
                .accept(acceptedContentType)
                .get(ROOT_PATH + MEDIA_TYPE_PATH)
                .then().header(CONTENT_TYPE, expectedContentType);
    }

    //    private ValidatableResponse req99BottlesOfBeer(int bottleNumber, int httpStatusCode) {
    //        return app.given()
    //                .get(ROOT_PATH + NinetyNineBottlesOfBeerResource.PATH + "/" + bottleNumber)
    //                .then().statusCode(httpStatusCode);
    //    }

    protected Protocol getProtocol() {
        return Protocol.HTTPS;
    }

    private String getAppEndpoint() {
        return app.getHost(getProtocol()) + ":" + app.getPort(getProtocol()) + ROOT_PATH;
    }

    private ResponsePredicateResult isHttp2x(HttpResponse<Void> resp) {
        return (resp.version().compareTo(HttpVersion.HTTP_2) == 0) ? ResponsePredicateResult.success()
                : ResponsePredicateResult.failure("Expected HTTP/2");
    }

    private WebClientOptions defaultVertxHttpClientOptions() throws URISyntaxException {
        return new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2).setSsl(true).setVerifyHost(false)
                .setUseAlpn(true)
                .setTrustStoreOptions(new JksOptions().setPassword(PASSWORD).setPath(defaultTruststore()));
    }

    private String defaultTruststore() throws URISyntaxException {
        URL res = getClass().getClassLoader().getResource(KEY_STORE_PATH);
        return Paths.get(res.toURI()).toFile().getAbsolutePath();
    }
}
