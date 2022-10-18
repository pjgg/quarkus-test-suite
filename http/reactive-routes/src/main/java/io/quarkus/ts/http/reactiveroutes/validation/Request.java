package io.quarkus.ts.http.reactiveroutes.validation;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.quarkus.ts.http.reactiveroutes.validation.annotations.Uppercase;

public class Request {
    @Size(min = 3, max = 3, message = "First code must have 3 characters")
    private String firstCode;

    @Pattern(regexp = "[A-Z]{2}[0-9]{3}", message = "Second second must match pattern")
    private String secondCode;

    @Uppercase
    private String custom;

    public String getFirstCode() {
        return firstCode;
    }

    public void setFirstCode(String firstCode) {
        this.firstCode = firstCode;
    }

    public String getSecondCode() {
        return secondCode;
    }

    public void setSecondCode(String secondCode) {
        this.secondCode = secondCode;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }
}
