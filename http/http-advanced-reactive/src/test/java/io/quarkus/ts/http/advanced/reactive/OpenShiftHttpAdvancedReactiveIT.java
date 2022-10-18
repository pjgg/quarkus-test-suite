package io.quarkus.ts.http.advanced.reactive;

import io.quarkus.test.bootstrap.KeycloakService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.services.QuarkusApplication;

@OpenShiftScenario
public class OpenShiftHttpAdvancedReactiveIT extends BaseHttpAdvancedReactiveIT {

    private static final String REALM_DEFAULT = "test-realm";

    //TODO Remove workaround after Keycloak is fixed https://github.com/keycloak/keycloak/issues/9916
    @KeycloakContainer(command = { "start-dev --import-realm --hostname-strict=false" })
    static KeycloakService keycloak = new KeycloakService("/keycloak-realm.json", REALM_DEFAULT, "/realms")
            .withProperty("JAVA_OPTS", "-Dcom.redhat.fips=false");

    @QuarkusApplication(ssl = true)
    static RestService app = new RestService().withProperty("quarkus.oidc.auth-server-url",
            keycloak::getRealmUrl);

    @Override
    protected RestService getApp() {
        return app;
    }

    @Override
    protected Protocol getProtocol() {
        // HTTPs is not supported in OpenShift yet. The same happens in OpenShift TS.
        return Protocol.HTTP;
    }
}
