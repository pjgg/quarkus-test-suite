package io.quarkus.ts.monitoring.opentracing.reactive.grpc.pong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.ts.monitoring.opentracing.reactive.grpc.traceable.TraceableResource;
import io.smallrye.mutiny.Uni;

@Path("/reactive-pong")
public class ReactivePongResource extends TraceableResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getPong() {
        recordTraceId();
        return Uni.createFrom().item("pong");
    }
}