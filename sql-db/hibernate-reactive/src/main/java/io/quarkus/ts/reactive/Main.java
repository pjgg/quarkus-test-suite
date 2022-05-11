package io.quarkus.ts.reactive;

import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String... args) {
        LOGGER.info("Running main method");
        Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
        Quarkus.run(args);
    }
}
