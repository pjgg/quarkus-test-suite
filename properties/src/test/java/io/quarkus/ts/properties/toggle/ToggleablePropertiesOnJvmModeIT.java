package io.quarkus.ts.properties.toggle;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.QuarkusApplication;
import io.restassured.specification.RequestSpecification;

@QuarkusScenario
public class ToggleablePropertiesOnJvmModeIT extends BaseToggleablePropertiesIT {

    @QuarkusApplication
    static RestService app = new RestService();

    @Override
    protected RequestSpecification given() {
        return app.given();
    }

    @Override
    protected void whenChangeServiceAtRuntime(ToggleableServices service, boolean enable) {
        app.stop();
        app.withProperty(service.getToggleProperty(), "" + enable);
        app.start();
    }
}
