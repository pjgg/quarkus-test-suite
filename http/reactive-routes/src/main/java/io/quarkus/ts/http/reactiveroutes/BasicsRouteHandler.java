package io.quarkus.ts.http.reactiveroutes;

import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteBase;

@RouteBase(path = "/basics")
public class BasicsRouteHandler {
    @Route(methods = HttpMethod.GET, path = "/param-with-underscore/:first_param")
    boolean validateRequestSingleParam(@Param("first_param") String param) {
        return true;
    }
}
