package io.quarkus.ts.http.hibernate.validator;

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.ts.http.hibernate.validator.sources.ClassicResource;
import io.quarkus.ts.http.hibernate.validator.sources.EventDTO;

@QuarkusScenario
public class ResteasyClassicUsingJsonIT extends BaseResteasyIT {

    @QuarkusApplication(classes = { ClassicResource.class, EventDTO.class }, dependencies = {
            @Dependency(artifactId = "quarkus-resteasy-jackson")
    })
    static final RestService app = new RestService();

    @Test
    public void validateDefaultMediaType() {
        validate(CLASSIC_ENDPOINT_WITH_NO_PRODUCES)
                .isBadRequest()
                .hasClassicJsonError();
    }

    // https://github.com/quarkusio/quarkus/issues/29208
    @Test
    public void validateCloudEventsWithHibernateValidatorOnNativeMode() {
        EventDTO event = new EventDTO();
        event.setCallUUID(UUID.randomUUID().toString());
        event.setMessage("msg from ResteasyClassicUsingJsonIT");
        event.setIpAddress("192.168.2.1");
        event.setServiceUUID(UUID.randomUUID().toString());
        event.setTimestamp((new Timestamp(System.currentTimeMillis())).toString());
        validateWithBody(CLASSIC_ENDPOINT_NATIVE_HIBERNATE_VALIDATOR, event).isOkRequest();
    }
}
