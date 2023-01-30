package io.quarkus.ts.opentelemetry.grpc;

import org.jboss.logmanager.MDC;

import io.grpc.stub.StreamObserver;
import io.quarkus.example.LastTraceIdReply;
import io.quarkus.example.LastTraceIdRequest;
import io.quarkus.example.PongReply;
import io.quarkus.example.PongRequest;
import io.quarkus.example.PongServiceGrpc;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class GrpcPongService extends PongServiceGrpc.PongServiceImplBase {

    private String lastTraceId;

    @Override
    public void sayPong(PongRequest request, StreamObserver<PongReply> responseObserver) {
        lastTraceId = MDC.get("traceId");
        responseObserver.onNext(PongReply.newBuilder().setMessage("pong").build());
        responseObserver.onCompleted();
    }

    @Override
    public void returnLastTraceId(LastTraceIdRequest request, StreamObserver<LastTraceIdReply> responseObserver) {
        responseObserver.onNext(LastTraceIdReply.newBuilder().setMessage(getLastTraceId()).build());
        responseObserver.onCompleted();
    }

    public String getLastTraceId() {
        return lastTraceId;
    }
}
