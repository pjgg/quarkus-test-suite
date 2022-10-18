package io.quarkus.ts.monitoring.opentracing.reactive.grpc.pong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.ts.monitoring.opentracing.reactive.grpc.traceable.TraceableResource;

@Path("/rest-pong")
public class PongResource extends TraceableResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPong() {
        recordTraceId();
        return "pong";
    }
}