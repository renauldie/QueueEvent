package com.renauldiedev.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();
        String query = request.getURI().getRawQuery();
        String fullPath = query != null ? path + "?" + query : path;

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String clientIp = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";

        log.info("[INBOUND] CorrelationId: [{}] {} {} from IP: {}", correlationId, method, fullPath, clientIp);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    int status = statusCode != null ? statusCode.value() : 0;

                    log.info("[OUTBOUND] CorrelationId: [{}] {} {} -> HTTP {} ({} ms)",
                            correlationId, method, fullPath, status, duration);
                });
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
