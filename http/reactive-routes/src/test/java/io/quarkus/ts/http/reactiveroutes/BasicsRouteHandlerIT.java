package io.quarkus.ts.http.reactiveroutes;

import static io.restassured.RestAssured.given;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.scenarios.QuarkusScenario;

@QuarkusScenario
public class BasicsRouteHandlerIT {

    @Test
    public void shouldWorkUsingParamsWithUnderscore() {
        given().when()
                .get("/basics/param-with-underscore/work")
                .then()
                .statusCode(HttpStatus.SC_OK);
    }
}
