package io.quarkus.ts.spring.web.reactive.openapi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vertx.ext.web.handler.sockjs.impl.StringEscapeUtils;

@RestController
@RequestMapping(value = "/put-text-plain", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
public class PutTextPlainController {

    @PutMapping
    public String hello(@RequestBody String body) throws Exception {
        return StringEscapeUtils.escapeJava(body);
    }
}
