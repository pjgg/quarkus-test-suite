package io.quarkus.ts.http.jaxrs;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path("/multipart")
public class MultipartResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @MultipartForm
    public MultipartBody postForm(@MultipartForm MultipartBody multipartBody) {
        return multipartBody;
    }

    @POST
    @Path("/text")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String postFormReturnText(@MultipartForm MultipartBody multipartBody) {
        return multipartBody.text;
    }

    @POST
    @Path("/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] postFormReturnFile(@MultipartForm MultipartBody multipartBody) throws IOException {
        return IOUtils.toByteArray(multipartBody.image);
    }

    @POST
    @Path("/data")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] postFormReturnData(@MultipartForm MultipartBody multipartBody) throws IOException {
        return IOUtils.toByteArray(multipartBody.data);
    }

    @POST
    @Path("/echo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String echo(String requestBody) throws Exception {
        return requestBody;
    }
}
