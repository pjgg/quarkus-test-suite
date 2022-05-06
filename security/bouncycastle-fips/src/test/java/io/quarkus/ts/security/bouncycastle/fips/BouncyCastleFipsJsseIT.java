package io.quarkus.ts.security.bouncycastle.fips;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.Dependency;
import io.quarkus.test.services.QuarkusApplication;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.ext.web.client.WebClient;

@QuarkusScenario
// TODO https://github.com/quarkusio/quarkus/issues/25516
@DisabledOnJre(value = JRE.JAVA_17)
public class BouncyCastleFipsJsseIT {

    private static final String PASSWORD = "password";
    private static final String BCFIPS = BouncyCastleFipsProvider.PROVIDER_NAME;
    private static final String BCJSSE = "BCJSSE";
    private static final String KS_TYPE = "BCFKS";

    @QuarkusApplication(ssl = true, dependencies = {
            @Dependency(groupId = "org.bouncycastle", artifactId = "bctls-fips", version = "${bouncycastle.bctls-fips.version}")
    })
    private static final RestService app = new RestService().withProperties("jsse.properties");

    @Test
    public void verifyBouncyCastleFipsAndJsseProviderAvailability() throws Exception {
        Security.insertProviderAt(new BouncyCastleFipsProvider(), 1);
        WebClient webClient = WebClient.create(new io.vertx.mutiny.core.Vertx(Vertx.vertx()), createWebClientOptions());
        String endpoint = app.getHost(Protocol.HTTPS) + ":" + app.getPort(Protocol.HTTPS) + "/api/listProviders";
        String body = webClient
                .getAbs(endpoint)
                .sendAndAwait().bodyAsString();

        String expectedResp = String.join(",", List.of(BCFIPS, BCJSSE));
        assertThat(body, containsString(expectedResp));
    }

    private WebClientOptions createWebClientOptions() throws Exception {
        WebClientOptions webClientOptions = new WebClientOptions();

        byte[] keyStoreData = getFileContent(Paths.get("src", "test", "resources", "client-keystore.jks"));
        KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                .setPassword(PASSWORD)
                .setValue(Buffer.buffer(keyStoreData))
                .setType(KS_TYPE)
                .setProvider(BCFIPS);
        webClientOptions.setKeyCertOptions(keyStoreOptions);

        byte[] trustStoreData = getFileContent(Paths.get("src", "test", "resources", "client-truststore.jks"));
        KeyStoreOptions trustStoreOptions = new KeyStoreOptions()
                .setPassword(PASSWORD)
                .setValue(Buffer.buffer(trustStoreData))
                .setType(KS_TYPE)
                .setProvider(BCFIPS);
        webClientOptions.setVerifyHost(false).setTrustAll(true).setTrustOptions(trustStoreOptions);

        return webClientOptions;
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }
}
