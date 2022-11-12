package io.quarkus.ts.http.hibernate.validator.sources;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import io.quarkus.runtime.annotations.RegisterForReflection;

@XmlRootElement
@RegisterForReflection(serialization = true)
public class EventDTO implements Serializable {

    private static final long serialVersionUID = 1305278483346223763L;

    @NotNull(message = "Call UUID value should be not null")
    @NotBlank(message = "Call UUID may not be blank")
    private String callUUID;

    @NotNull(message = "Timestamp value should be not null")
    @NotBlank(message = "Timestamp may not be blank")
    private String timestamp;

    @NotNull(message = "Service UUID value should be not null")
    @NotBlank(message = "Service UUID may not be blank")
    private String serviceUUID;

    @NotNull(message = "IP Address value should be not null")
    @NotBlank(message = "IP Address may not be blank")
    private String ipAddress;

    private String message;

    public String getCallUUID() {
        return callUUID;
    }

    public void setCallUUID(String callUUID) {
        this.callUUID = callUUID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getServiceUUID() {
        return serviceUUID;
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
