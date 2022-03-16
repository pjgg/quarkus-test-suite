package io.quarkus.ts.stork;

import static io.quarkus.ts.stork.PongReplicaResource.DEFAULT_PONG_REPLICA_RESPONSE;
import static io.quarkus.ts.stork.PongResource.DEFAULT_PONG_RESPONSE;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.bootstrap.ConsulService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Container;
import io.quarkus.test.services.QuarkusApplication;

@QuarkusScenario
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StorkServiceDiscoveryIT {

    @Container(image = "${consul.image}", expectedLog = "Synced node info", port = 8500)
    static ConsulService consul = new ConsulService();

    private static final String PREFIX = "ping-";
    private static final String PUNG_PORT = getAvailablePort();
    private static final String PONG_PORT = getAvailablePort();
    private static final String PONG_REPLICA_PORT = getAvailablePort();

    @QuarkusApplication(classes = PungResource.class)
    static RestService pungService = new RestService()
            .withProperty("quarkus.http.port", PUNG_PORT)
            .withProperty("pung-service-port", PUNG_PORT)
            .withProperty("pung-service-host", "localhost")
            .withProperty("stork.pung.service-discovery", "consul")
            .withProperty("stork.pung.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pung.service-discovery.consul-host", () -> getConsultEndpoint(consul.getConsulEndpoint()));

    @QuarkusApplication(classes = PongResource.class)
    static RestService pongService = new RestService()
            .withProperty("quarkus.http.port", PONG_PORT)
            .withProperty("pong-service-port", PONG_PORT)
            .withProperty("pong-service-host", "localhost")
            .withProperty("stork.pong.service-discovery.refresh-period", "1")
            .withProperty("stork.pong.service-discovery", "consul")
            .withProperty("stork.pong.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pong.service-discovery.consul-host", () -> getConsultEndpoint(consul.getConsulEndpoint()));

    @QuarkusApplication(classes = PongReplicaResource.class)
    static RestService pongReplicaService = new RestService()
            .withProperty("quarkus.http.port", PONG_REPLICA_PORT)
            .withProperty("pong-replica-service-port", PONG_REPLICA_PORT)
            .withProperty("pong-replica-service-host", "localhost")
            .withProperty("stork.pong-replica.service-discovery.refresh-period", "1")
            .withProperty("stork.pong-replica.service-discovery", "consul")
            .withProperty("stork.pong-replica.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pong-replica.service-discovery.consul-host",
                    () -> getConsultEndpoint(consul.getConsulEndpoint()));

    @QuarkusApplication(classes = { PingResource.class, MyBackendPungProxy.class, MyBackendPongProxy.class })
    static RestService mainPingService = new RestService()
            .withProperty("stork.pung.service-discovery", "consul")
            .withProperty("stork.pung.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pung.service-discovery.consul-host", () -> getConsultEndpoint(consul.getConsulEndpoint()))
            .withProperty("stork.pong-replica.service-discovery", "consul")
            .withProperty("stork.pong-replica.service-discovery.refresh-period", "1")
            .withProperty("stork.pong-replica.load-balancer", "round-robin")
            .withProperty("stork.pong-replica.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pong-replica.service-discovery.consul-host",
                    () -> getConsultEndpoint(consul.getConsulEndpoint()))
            .withProperty("stork.pong.service-discovery", "consul")
            .withProperty("stork.pong.service-discovery.refresh-period", "1")
            .withProperty("stork.pong.load-balancer", "round-robin")
            .withProperty("stork.pong.service-discovery.consul-port", () -> String.valueOf(consul.getPort()))
            .withProperty("stork.pong.service-discovery.consul-host", () -> getConsultEndpoint(consul.getConsulEndpoint()));

    @Test
    @Order(1)
    public void invokeServiceByName() {
        String response = makePingCall("pung");
        assertThat("Service discovery by name fail.", PREFIX + "pung", is(response));
    }

    @Test
    @Order(2)
    public void storkLoadBalancerByRoundRobin() {
        Map<String, Integer> uniqueResp = new HashMap<>();
        final int requestAmount = 100;
        final int roundRobinError = (requestAmount / 2) - 1;
        for (int i = 0; i < requestAmount; i++) {
            String response = makePingCall("pong");
            if (uniqueResp.containsKey(response)) {
                uniqueResp.put(response, uniqueResp.get(response) + 1);
            } else {
                uniqueResp.put(response, 1);
            }
        }

        Assertions.assertEquals(uniqueResp.size(), 2, "Only 2 services should response");
        assertThat("Unexpected service names", uniqueResp.keySet(),
                hasItems(PREFIX + DEFAULT_PONG_RESPONSE, PREFIX + DEFAULT_PONG_REPLICA_RESPONSE));
        assertThat("Load balancer doesn't behaves as round-robin", uniqueResp.get(PREFIX + DEFAULT_PONG_RESPONSE),
                is(greaterThanOrEqualTo(roundRobinError)));
        assertThat("Load balancer doesn't behaves as round-robin", uniqueResp.get(PREFIX + DEFAULT_PONG_REPLICA_RESPONSE),
                is(greaterThanOrEqualTo(roundRobinError)));
    }

    @Test
    @Order(3) // run this test always last, because we are dropping pongService
    @Disabled("Looks that service-discovery.refresh-period is not aware about service readiness")
    //TODO https://github.com/quarkusio/quarkus/issues/24343
    public void storkLoadBalancerServiceNodeDown() {
        final int requestAmount = 100;
        final int nodeDownThreshold = 50;
        final Duration nodeDownRecoveryTimeout = Duration.ofSeconds(10);
        for (int i = 0; i < requestAmount; i++) {
            Awaitility.await().atMost(nodeDownRecoveryTimeout).untilAsserted(() -> makePingCall("pong"));
            if (i > nodeDownThreshold) {
                pongService.stop();
            }
        }

        String respBody = makePingCall("pong");
        Assertions.assertEquals(respBody, PREFIX + DEFAULT_PONG_REPLICA_RESPONSE, "Unexpected service name");
    }

    private String makePingCall(String subPath) {
        return mainPingService
                .given()
                .get("/ping/" + subPath).then()
                .statusCode(HttpStatus.SC_OK)
                .extract().body().asString();
    }

    private static String getConsultEndpoint(String endpoint) {
        return endpoint.replaceFirst(":\\d+", "");
    }

    private static String getAvailablePort() {
        int port = 0;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            fail("no free port available.");
        }

        return String.valueOf(port);
    }
}
