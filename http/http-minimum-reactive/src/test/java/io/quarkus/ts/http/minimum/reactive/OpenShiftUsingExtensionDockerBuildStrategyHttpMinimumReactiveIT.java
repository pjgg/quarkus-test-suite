package io.quarkus.ts.http.minimum.reactive;

import org.junit.jupiter.api.Tag;

import io.quarkus.test.scenarios.OpenShiftDeploymentStrategy;
import io.quarkus.test.scenarios.OpenShiftScenario;

@Tag("use-quarkus-openshift-extension")
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy)
public class OpenShiftUsingExtensionDockerBuildStrategyHttpMinimumReactiveIT extends HttpMinimumReactiveIT {

}