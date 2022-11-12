package io.quarkus.ts.http.cloudevents;

import java.net.URI;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

@Path("/events")
public class EventResource {

    @Inject
    ObjectMapper mapper;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/json")
    public EventDTO jsonPayload(@Valid EventDTO eventDTO) {

        // checking CloudEvent marshalling
        CloudEvent event = CloudEventBuilder.v1()
                .withSource(URI.create("example"))
                .withType("io.cloudevents.examples.quarkus.event")
                .withId(UUID.randomUUID().toString())
                .withDataContentType(MediaType.APPLICATION_JSON)
                .withData(eventDTO.toCloudEventData(mapper))
                .build();

        // checking the CloudEvent unmarshalling
        return EventDTO.fromCloudEvent(mapper, event);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/pureCE")
    public EventDTO pureCloudEventPayload(@Valid CloudEvent event) {
        if (event == null || event.getData() == null) {
            throw new BadRequestException("Invalid data received. Null or empty event");
        }

        return EventDTO.fromCloudEvent(mapper, event);
    }
}
