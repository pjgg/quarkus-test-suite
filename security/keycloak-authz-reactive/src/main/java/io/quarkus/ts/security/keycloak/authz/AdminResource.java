package io.quarkus.ts.security.keycloak.authz;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/admin")
public class AdminResource {
    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken jwt;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> get() {
        return Uni.createFrom().item("Hello, admin " + identity.getPrincipal().getName());
    }

    @GET
    @Path("/issuer")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> issuer() {
        return Uni.createFrom().item("admin token issued by " + jwt.getIssuer());
    }
}
