package io.quarkus.ts.messaging.kafka;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

@Path("/stock-price")
public class StockPriceEndpoint {

    @Inject
    @Channel("source-stock-price")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<StockPrice> stockPriceEmitter;

    @Inject
    @Channel("price-stream")
    Publisher<String> stockPrices;

    @Inject
    @Channel("price-stream-batch")
    Publisher<List<String>> stockPricesBatch;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<String> stream() {
        return stockPrices;
    }

    @GET
    @Path("/stream-batch")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<List<String>> streamBatch() {
        return stockPricesBatch;
    }

    @POST
    public Response addStockPrice(StockPriceDto stockPrice) {
        stockPriceEmitter.send(stockPrice.toAvro());
        return Response.accepted().build();
    }
}
