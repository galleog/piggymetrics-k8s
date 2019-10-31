package com.github.galleog.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * Slmple logger for gRPC incoming calls before that are dispatched by a {@link ServerCallHandler}.
 */
@Slf4j
public class LogServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        logger.info("gRPC incoming call to {}", call.getMethodDescriptor().getFullMethodName());
        return next.startCall(call, headers);
    }
}