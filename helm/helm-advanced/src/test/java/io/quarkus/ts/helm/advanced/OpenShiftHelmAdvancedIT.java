package io.quarkus.ts.helm.advanced;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusHelmClient;
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.scenarios.annotations.DisabledOnNative;
import io.restassured.RestAssured;

@OpenShiftScenario
// Helm is concerned just about image name, Native compilation is not relevant
@DisabledOnNative
public class OpenShiftHelmAdvancedIT {

    private static final int EXPECTED_SIZE = 7;
    private static final String PLATFORM_OPENSHIFT = "openshift";
    private static final String CHART_NAME = "my-chart";
    private static final String APP_SERVICE_NAME = "ts-quarkus-helm-advanced-app";
    private static final int TIMEOUT_MIN = 1;
    private static String APP_URL;

    @Inject
    static QuarkusHelmClient helmClient;

    @Inject
    static OpenShiftClient ocpClient;

    @BeforeAll
    public static void tearUp() {
        installChart(CHART_NAME);
    }

    @AfterAll
    public static void tearDown() {
        helmClient.uninstallChart(CHART_NAME);
    }

    @Test
    public void getAll() {
        RestAssured.given().baseUri(APP_URL)
                .get("/book")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("", hasSize(EXPECTED_SIZE));
    }

    private static void installChart(String chartName) {
        String chartFolderName = helmClient.getWorkingDirectory().getAbsolutePath() + "/helm/" + PLATFORM_OPENSHIFT + "/"
                + chartName;
        helmClient.run("dependency", "update", chartFolderName);
        QuarkusHelmClient.Result chartResultCmd = helmClient.installChart(chartName, chartFolderName);
        thenSucceed(chartResultCmd);

        APP_URL = ocpClient.url(APP_SERVICE_NAME).getRestAssuredStyleUri();

        await().ignoreExceptions().atMost(TIMEOUT_MIN, TimeUnit.MINUTES)
                .untilAsserted(() -> RestAssured.given().baseUri(APP_URL).get("/q/health/live")
                        .then().statusCode(HttpStatus.SC_OK));
    }

    private static void thenSucceed(QuarkusHelmClient.Result chartResultCmd) {
        assertTrue(
                chartResultCmd.isSuccessful(),
                String.format("Command %s fails", chartResultCmd.getCommandExecuted()));
    }
}
