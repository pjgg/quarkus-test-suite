package io.quarkus.ts.security.vertx.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;

public class NotFoundException extends RuntimeException {
    public int getHttpErrorCode() {
        return HttpResponseStatus.NOT_FOUND.code();
    }

    public NotFoundException(String message) {
        super(message);
    }
}
