package io.quarkus.ts.http.advanced.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.services.URILike;
import io.quarkus.test.utils.AwaitilityUtils;

public abstract class AbstractDevModeIT {
    private static final Logger LOG = Logger.getLogger(AbstractDevModeIT.class);
    protected static final String PROPERTY = "qe.test.value";
    protected WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = new WebClient();

        webClient.getOptions().setRedirectEnabled(true); //required for the test case
        //The execution breaks without the option below
        webClient.getOptions().setWebSocketEnabled(false);

        //make sure, that the cache doesn't affect us
        webClient.getCookieManager().clearCookies();
        webClient.getCookieManager().setCookiesEnabled(false);
        webClient.getCache().clear();
        webClient.getCache().setMaxSize(0);

        //disable everything, that we don't need
        webClient.getOptions().setDownloadImages(false);
        webClient.getOptions().setGeolocationEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setCssEnabled(false);
    }

    @Test
    @Disabled("https://github.com/quarkusio/quarkus/issues/30511")
    public void uiChange() throws IOException {
        RestService app = getApp();
        URILike uri = getUri();

        HtmlPage before = webClient.getPage(uri.withPath("/q/dev/io.quarkus.quarkus-vertx-http/config").toString());
        QuarkusUIField field = new QuarkusUIField(before.getElementById(PROPERTY));
        assertEquals("42", field.getValue(), "Wrong initial value shown in UI!");
        assertEquals("42", app.getProperty(PROPERTY, ""), "Properties contain wrong initial value!");

        field.setValue("23");
        HtmlPage saved = field.getSaveButton().click();
        QuarkusUIField updated = new QuarkusUIField(saved.getElementById(PROPERTY));
        assertEquals("23", updated.getValue(), "The value was not updated in UI");

        AwaitilityUtils.untilIsTrue(() -> app.getLogs().stream().anyMatch(log -> log.contains("File change detected")));
        try (Stream<String> lines = Files
                .lines(app.getServiceFolder().resolve("src/main/resources/application.properties"))) {
            List<String> properties = lines
                    .filter(line -> line.contains(PROPERTY))
                    .collect(Collectors.toList());
            if (properties.size() != 1) {
                LOG.warn("There should be only one property with name " + PROPERTY + ", but found these " + properties);
            }
        }
        assertEquals("23", app.getProperty(PROPERTY, ""), "Wrong value was read from application properties");
    }

    protected abstract URILike getUri();

    protected abstract RestService getApp();

    @AfterEach
    void tearDown() {
        webClient.close();
    }

    protected class QuarkusUIField {
        private final DomElement element;

        public QuarkusUIField(DomElement element) {
            this.element = element;
        }

        public String getValue() {
            return element.getAttribute("value");
        }

        public void setValue(String newValue) {
            element.setAttribute("value", newValue);
        }

        public DomElement getSaveButton() {
            for (DomElement sibling : element.getParentNode().getDomElementDescendants()) {
                if (sibling.getAttribute("class").equals("input-group-text formInputButton")) {
                    return sibling;
                }
            }
            throw new IllegalStateException("Save button was not found!");
        }
    }
}
