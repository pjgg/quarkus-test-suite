package io.quarkus.ts.cache.caffeine;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.common.annotation.Blocking;

@Blocking
@Path("/api/blocking")
public class BlockingWithCacheResource {

    private static final String CACHE_NAME = "api-blocking-cache";

    private static int counter = 0;

    @GET
    @CacheResult(cacheName = CACHE_NAME)
    public String getValue() {
        return "Value: " + counter++;
    }

    @POST
    @Path("/invalidate-cache")
    @CacheInvalidate(cacheName = CACHE_NAME)
    public void invalidate() {
        // do nothing
    }

    @GET
    @Path("/using-prefix/{prefix}")
    @CacheResult(cacheName = CACHE_NAME)
    public String getValueWithPrefix(@PathParam("prefix") @CacheKey String prefix) {
        return prefix + ": " + counter++;
    }

    @POST
    @Path("/using-prefix/{prefix}/invalidate-cache")
    @CacheInvalidate(cacheName = CACHE_NAME)
    public void invalidateWithPrefix(@PathParam("prefix") @CacheKey String prefix) {
        // do nothing
    }

    @POST
    @Path("/invalidate-cache-all")
    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public void invalidateAll() {
        // do nothing
    }
}
