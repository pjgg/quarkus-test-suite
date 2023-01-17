package io.quarkus.ts.http.advanced;

import java.time.Duration;
import java.util.Objects;

import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {
    private static final String TEMPLATE = "Hello, %s!";
    public static final int EVENT_PROPAGATION_WAIT_MS = 250;
    private String contextValueAfterEventPropagation;

    @Inject
    Event<String> event;

    @Inject
    LocalCustomContext localCustomContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Hello get(@QueryParam("name") @DefaultValue("World") String name) {
        return new Hello(String.format(TEMPLATE, name));
    }

    @PUT()
    @Path("/local-context/{value}")
    public void updateContext(@PathParam("value") String value) {
        event.fireAsync(value);
    }

    @GET()
    @Path("/local-context/{value}")
    public String retrieveContextValue(@PathParam("value") String value) {
        if (Objects.isNull(contextValueAfterEventPropagation)) {
            throw new NotFoundException(String.format("Resource %s not found!", value));
        }

        return contextValueAfterEventPropagation;
    }

    void dequeueProcessingRequests(@ObservesAsync String value) {
        localCustomContext.set(value);
        // 'wait' is required in order to reproduce the issue https://github.com/quarkusio/quarkus/issues/29017
        wait(Duration.ofMillis(EVENT_PROPAGATION_WAIT_MS));
        contextValueAfterEventPropagation = localCustomContext.get();
    }

    private void wait(Duration timeout) {
        try {
            Thread.sleep(timeout.toMillis());
        } catch (Exception e) {
        }
    }
}
