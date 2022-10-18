package io.quarkus.ts.messaging.infinispan.grpc.kafka.books;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Book.class }, schemaPackageName = "book_sample")
interface BookContextInitializer extends SerializationContextInitializer {
}
