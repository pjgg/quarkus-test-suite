package io.quarkus.ts.many.extensions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ServiceClammy {
    public String process(String name) {
        return "Clammy - " + name + " - done";
    }
}
