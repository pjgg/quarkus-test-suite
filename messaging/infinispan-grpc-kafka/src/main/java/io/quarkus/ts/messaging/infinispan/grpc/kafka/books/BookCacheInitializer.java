package io.quarkus.ts.messaging.infinispan.grpc.kafka.books;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class BookCacheInitializer {

    public static final String CACHE_NAME = "booksCache";

    @Inject
    RemoteCacheManager cacheManager;

    private static final String CACHE_CONFIG = "<infinispan><cache-container>" +
            "<distributed-cache name=\"%s\"></distributed-cache>" +
            "</cache-container></infinispan>";

    void onStart(@Observes StartupEvent ev) {
        cacheManager.administration().getOrCreateCache(CACHE_NAME,
                new XMLStringConfiguration(String.format(CACHE_CONFIG, CACHE_NAME)));

    }

}
