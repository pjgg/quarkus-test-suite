package io.quarkus.qe.messaging.infinispan;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.InfinispanService;
import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
@Disabled
public class InfinispanKafkaSaslIT {
    /**
     * We can't rename this file to use the default SSL settings part of KafkaService.
     */
    private static final String BOOK_TITLE = "testBook";

    @Container(image = "${infinispan.image}", expectedLog = "${infinispan.expected-log}", port = 11222)
    static final InfinispanService infinispan = new InfinispanService()
            .withConfigFile("infinispan-config.yaml")
            .withSecretFiles("server.jks");

    @KafkaContainer(vendor = KafkaVendor.STRIMZI, protocol = KafkaProtocol.SASL)
    static final KafkaService kafkasasl = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.infinispan-client.server-list", infinispan::getInfinispanServerAddress)
            .withProperty("quarkus.infinispan-client.auth-username", infinispan.getUsername())
            .withProperty("quarkus.infinispan-client.auth-password", infinispan.getPassword())
            .withProperty("quarkus.infinispan-client.trust-store", "secret::/server.jks")
            .withProperty("quarkus.infinispan-client.trust-store-password", "changeit")
            .withProperty("quarkus.infinispan-client.trust-store-type", "jks")
            .withProperty("kafka-streams.state.dir", "target")
            .withProperty("kafka-client-sasl.bootstrap.servers", kafkasasl::getBootstrapUrl);

    @Test
    void testKafkaClientSASL() {
        await().untilAsserted(() -> {
            given()
                    .queryParam("key", "my-key")
                    .queryParam("value", "my-value")
                    .when()
                    .post("/kafka/sasl")
                    .then()
                    .statusCode(200);

            get("/kafka/sasl")
                    .then()
                    .statusCode(200)
                    .body(StringContains.containsString("my-key-my-value"));
        });

        get("/kafka/sasl/topics")
                .then()
                .statusCode(200)
                .body(StringContains.containsString("hello"));
    }
}
