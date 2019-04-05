package com.github.galleog.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple logger for gRPC outgoing calls before they are dispatched by a {@link Channel}.
 */
@Slf4j
public class LogClientInterceptor implements ClientInterceptor {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        logger.info("gRPC outgoing call to {}", method.getFullMethodName());
        return next.newCall(method, callOptions);
    }
}
