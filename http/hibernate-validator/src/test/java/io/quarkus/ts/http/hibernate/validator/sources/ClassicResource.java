package io.quarkus.ts.http.hibernate.validator.sources;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;

@Path("/classic")
public class ClassicResource {

    @GET
    @Path("/validate-no-produces/{id}")
    public String validateNoProduces(
            @Digits(integer = 5, fraction = 0, message = "numeric value out of bounds") @PathParam("id") String id) {
        return id;
    }

    @GET
    @Path("/validate-multiple-produces/{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    public String validateMultipleProduces(
            @Digits(integer = 5, fraction = 0, message = "numeric value out of bounds") @PathParam("id") String id) {
        return id;
    }

    @POST
    @Path("/validate-native-compilation")
    public EventDTO publishInitialEvent(@Valid EventDTO eventDTO) {

        CloudEventImpl<EventDTO> event = CloudEventBuilder.<EventDTO> builder()
                .withType("code.quarkus.io")
                .withId(UUID.randomUUID().toString())
                .withTime(ZonedDateTime.now())
                .withDataContentType("application/json")
                .withData(eventDTO)
                .withSource(URI.create("/event-producer"))
                //.withValidator(validator)
                .build();
        return event.getData().get();
    }
}
