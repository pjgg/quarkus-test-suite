package io.quarkus.ts.transactions;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.JaegerService;
import io.quarkus.test.services.JaegerContainer;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public abstract class TransactionCommons {

    static final String ACCOUNT_NUMBER_MIGUEL = "SK0389852379529966291984";
    static final String ACCOUNT_NUMBER_GARCILASO = "FR9317569000409377431694J37";
    static final String ACCOUNT_NUMBER_LUIS = "ES8521006742088984966816";
    static final String ACCOUNT_NUMBER_LOPE = "CZ9250512252717368964232";
    static final String ACCOUNT_NUMBER_FRANCISCO = "ES8521006742088984966817";

    static final String ACCOUNT_NUMBER_EDUARDO = "ES8521006742088984966899";
    static final int ASSERT_SERVICE_TIMEOUT_MINUTES = 1;
    private Response jaegerResponse;

    @JaegerContainer(useOtlpCollector = true, expectedLog = "\"Health Check state change\",\"status\":\"ready\"")
    static final JaegerService jaeger = new JaegerService();

    @Tag("QUARKUS-2492")
    @Test
    public void verifyNarayanaProgrammaticApproachTransaction() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_MIGUEL);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_LOPE);
        transferDTO.setAmount(100);

        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/transaction")
                .then().statusCode(HttpStatus.SC_CREATED);

        AccountEntity miguelAccount = getAccount(ACCOUNT_NUMBER_MIGUEL);
        Assertions.assertEquals(0, miguelAccount.getAmount(), "Unexpected amount on source account.");

        AccountEntity lopeAccount = getAccount(ACCOUNT_NUMBER_LOPE);
        Assertions.assertEquals(200, lopeAccount.getAmount(), "Unexpected amount on source account.");

        JournalEntity miguelJournal = getLatestJournalRecord(ACCOUNT_NUMBER_MIGUEL);
        Assertions.assertEquals(100, miguelJournal.getAmount(), "Unexpected journal amount.");
    }

    @Tag("QUARKUS-2492")
    @Test
    public void verifyLegacyNarayanaLambdaApproachTransaction() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_GARCILASO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_GARCILASO);
        transferDTO.setAmount(100);

        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/legacy/top-up")
                .then().statusCode(HttpStatus.SC_CREATED);

        AccountEntity garcilasoAccount = getAccount(ACCOUNT_NUMBER_GARCILASO);
        Assertions.assertEquals(200, garcilasoAccount.getAmount(),
                "Unexpected account amount. Expected 200 found " + garcilasoAccount.getAmount());

        JournalEntity garcilasoJournal = getLatestJournalRecord(ACCOUNT_NUMBER_GARCILASO);
        Assertions.assertEquals(100, garcilasoJournal.getAmount(), "Unexpected journal amount.");
    }

    @Tag("QUARKUS-2492")
    @Test
    public void verifyNarayanaLambdaApproachTransaction() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_EDUARDO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_EDUARDO);
        transferDTO.setAmount(100);

        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/top-up")
                .then().statusCode(HttpStatus.SC_CREATED);

        AccountEntity garcilasoAccount = getAccount(ACCOUNT_NUMBER_EDUARDO);
        Assertions.assertEquals(200, garcilasoAccount.getAmount(),
                "Unexpected account amount. Expected 200 found " + garcilasoAccount.getAmount());

        JournalEntity garcilasoJournal = getLatestJournalRecord(ACCOUNT_NUMBER_EDUARDO);
        Assertions.assertEquals(100, garcilasoJournal.getAmount(), "Unexpected journal amount.");
    }

    @Tag("QUARKUS-2492")
    @Test
    public void verifyRollbackForNarayanaProgrammaticApproach() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_LUIS);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_LUIS);
        transferDTO.setAmount(200);

        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);

        AccountEntity luisAccount = getAccount(ACCOUNT_NUMBER_LUIS);
        Assertions.assertEquals(100, luisAccount.getAmount(), "Unexpected account amount.");

        given().get("/transfer/journal/latest/" + ACCOUNT_NUMBER_LUIS)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Tag("QUARKUS-2492")
    @Test
    public void smokeTestNarayanaProgrammaticTransactionTrace() {
        String operationName = "/transfer/accounts/{account_id}";
        given().get("/transfer/accounts/" + ACCOUNT_NUMBER_LUIS).then().statusCode(HttpStatus.SC_OK);
        verifyRestRequestTraces(operationName);
    }

    @Tag("QUARKUS-2492")
    @Test
    public void smokeTestMetricsNarayanaProgrammaticTransaction() {
        String metricName = "transaction_withdrawal_amount";
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setAccountFrom(ACCOUNT_NUMBER_FRANCISCO);
        transferDTO.setAccountTo(ACCOUNT_NUMBER_FRANCISCO);
        transferDTO.setAmount(20);

        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_CREATED);

        verifyMetrics(metricName, greater(0));

        // check rollback gauge
        transferDTO.setAmount(3000);
        double beforeRollback = getMetricsValue(metricName);
        given()
                .contentType(ContentType.JSON)
                .body(transferDTO).post("/transfer/withdrawal")
                .then().statusCode(HttpStatus.SC_BAD_REQUEST);
        double afterRollback = getMetricsValue(metricName);
        Assertions.assertEquals(beforeRollback, afterRollback, "Gauge should not be increased on a rollback transaction");
    }

    private AccountEntity getAccount(String accountNumber) {
        return given().get("/transfer/accounts/" + accountNumber)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .body().as(AccountEntity.class);
    }

    private void verifyRestRequestTraces(String operationName) {
        String[] operations = new String[] { operationName };
        await().atMost(1, TimeUnit.MINUTES).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            retrieveTraces(20, "1h", "narayanaTransactions", operationName);
            jaegerResponse.then().body("data[0].spans.operationName", containsInAnyOrder(operations));
        });
    }

    private JournalEntity getLatestJournalRecord(String accountNumber) {
        return given().get("/transfer/journal/latest/" + accountNumber)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .body().as(JournalEntity.class);
    }

    private void retrieveTraces(int pageLimit, String lookBack, String serviceName, String operationName) {
        jaegerResponse = given().when()
                .log().uri()
                .queryParam("operation", operationName)
                .queryParam("lookback", lookBack)
                .queryParam("limit", pageLimit)
                .queryParam("service", serviceName)
                .get(jaeger.getTraceUrl());
    }

    private void verifyMetrics(String name, Predicate<Double> valueMatcher) {
        await().ignoreExceptions().atMost(ASSERT_SERVICE_TIMEOUT_MINUTES, TimeUnit.MINUTES).untilAsserted(() -> {
            String response = given().get("/q/metrics").then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract().asString();

            boolean matches = false;
            for (String line : response.split("[\r\n]+")) {
                if (line.startsWith(name)) {
                    Double value = extractValueFromMetric(line);
                    Assertions.assertTrue(valueMatcher.test(value), "Metric " + name + " has unexpected value " + value);
                    matches = true;
                    break;
                }
            }

            Assertions.assertTrue(matches, "Metric " + name + " not found in " + response);
        });
    }

    private Double getMetricsValue(String name) {
        String response = given().get("/q/metrics").then().statusCode(HttpStatus.SC_OK).extract().asString();
        for (String line : response.split("[\r\n]+")) {
            if (line.startsWith(name)) {
                return extractValueFromMetric(line);
            }
        }

        Assertions.fail("Metrics property " + name + " not found.");
        return 0d;
    }

    private Double extractValueFromMetric(String line) {
        return Double.parseDouble(line.substring(line.lastIndexOf(" ")));
    }

    private Predicate<Double> greater(double expected) {
        return actual -> actual > expected;
    }

}
