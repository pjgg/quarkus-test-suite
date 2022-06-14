package io.quarkus.ts.http.restclient.reactive;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.ts.http.restclient.reactive.json.Book;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@RegisterRestClient
@Path("/books")
@RegisterClientHeaders
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public interface BookClient {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<Book> getBook(@QueryParam("title") String title, @QueryParam("author") String author);

    @Path("/author")
    AuthorClient getAuthor();

    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    interface AuthorClient {
        @GET
        @Path("/name")
        Uni<String> getName(@QueryParam("author") String author);

        @Path("profession")
        ProfessionClient getProfession();
    }

    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    interface ProfessionClient {
        @GET
        @Path("/title")
        Uni<String> getTitle();

        @Path("/wage")
        WageClient getWage();
    }

    interface WageClient {
        @GET
        @Path("/amount")
        String getAmount();

        @Path("/currency")
        CurrencyClient getCurrency();
    }

    interface CurrencyClient {
        @GET
        @Path("/name")
        String getName();
    }

    @GET
    @Path("/クイック検索/% # [ ] + = & @ : ! * ( ) ' $ , ?/- _ . ~")
    Multi<String> getByDecodedSearchTerm(@QueryParam("searchTerm") String searchTerm);

    @GET
    @Path("/%E3%82%AF%E3%82%A4%E3%83%83%E3%82%AF%E6%A4%9C%E7%B4%A2/%25%20%23%20%5B%20%5D%20+%20=%20&%20@%20:%20!%20*%20(%20)%20'%20$%20,%20%3F/-%20_%20.%20~")
    Multi<String> getByEncodedSearchTerm(@QueryParam("searchTerm") String searchTerm);

}
