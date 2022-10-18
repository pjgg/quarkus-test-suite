package io.quarkus.ts.http.reactiveroutes.validation;

import javax.validation.constraints.NotNull;

import io.quarkus.ts.http.reactiveroutes.validation.annotations.Uppercase;

public class Response {
    @NotNull(message = "id can't be null")
    private String id;

    @Uppercase
    private String custom;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }
}
