package io.quarkus.ts.http.cloudevents;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.sql.Timestamp;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusScenario
public class HttpCloudEventsIT {
    static final String EXPECTED_MSG = "msg from HttpCloudEventsIT";

    @Test
    public void verifyCloudEventsManualMarshalling() {
        EventDTO event = defaultEventDto();
        given().body(event)
                .contentType(ContentType.JSON)
                .post("/events/json")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("message", is(EXPECTED_MSG));
    }

    @Test
    public void verifyPureCloudEvents() {
        EventDTO event = defaultEventDto();
        makeCloudEventPost(event, "/events/pureCE")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("message", is(EXPECTED_MSG));
    }

    @Test
    @Disabled // TODO Hibernate-validator is not triggered
    public void verifyHibernateValidatorOnPureCloudEvents() {
        EventDTO event = defaultEventDto();
        event.setIpAddress(null);
        makeCloudEventPost(event, "/events/pureCE")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    private EventDTO defaultEventDto() {
        EventDTO event = new EventDTO();
        event.setCallUUID(UUID.randomUUID().toString());
        event.setMessage(EXPECTED_MSG);
        event.setIpAddress("192.168.2.1");
        event.setServiceUUID(UUID.randomUUID().toString());
        event.setTimestamp((new Timestamp(System.currentTimeMillis())).toString());
        return event;
    }

    private Response makeCloudEventPost(EventDTO event, String path) {
        return given().body(event)
                .contentType(ContentType.JSON)
                .header("Ce-Specversion", "1.0")
                .header("Ce-Type", "EventDTO")
                .header("Ce-Source", "io.quarkus.ts.http.cloudevents/eventDTO")
                .header("Ce-Id", UUID.randomUUID().toString())
                .header("Ce-Subject", "SUBJ-0001")
                .post(path);
    }

}
