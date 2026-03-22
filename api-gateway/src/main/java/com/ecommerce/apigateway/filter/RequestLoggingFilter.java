package com.ecommerce.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Global request/response logging filter for the API Gateway.
 *
 * <p>Runs at {@link #FILTER_ORDER} ({@value #FILTER_ORDER}), which is higher
 * priority (lower number) than {@link JwtAuthenticationFilter} ({@code -100}).
 * This ensures the logged duration covers the entire gateway round-trip,
 * including authentication time.
 *
 * <p>Log lines emitted:
 * <ul>
 *   <li>{@code GATEWAY_IN}  — on every inbound request</li>
 *   <li>{@code GATEWAY_OUT} — on every response (success or error)</li>
 *   <li>{@code SLOW_REQUEST} — additionally when duration exceeds {@code gateway.slow-request-threshold-ms}</li>
 *   <li>{@code SERVER_ERROR} — additionally on 5xx responses</li>
 * </ul>
 *
 * <p><strong>Security:</strong> query parameters whose names match
 * {@link #SENSITIVE_PARAMS} are replaced with {@code [REDACTED]} before logging
 * to prevent credentials and tokens from appearing in ELK.
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    // ── Constants ─────────────────────────────────────────────────────────────

    /**
     * Filter order — must be lower (higher priority) than
     * {@link JwtAuthenticationFilter FILTER_ORDER} so this filter wraps the
     * entire auth + routing cycle.
     */
    static final int FILTER_ORDER = -200;

    /**
     * Query parameter names that must never appear in logs.
     * Case-insensitive comparison is applied at runtime.
     */
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "token", "access_token", "refresh_token",
            "password", "secret", "api_key", "apikey",
            "authorization", "credential"
    );

    /**
     * Replaces sensitive query param values: {@code key=anything} → {@code key=[REDACTED]}.
     */
    private static final Pattern SENSITIVE_PARAM_PATTERN = buildSensitiveParamPattern();

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Requests taking longer than this threshold (in milliseconds) emit an
     * additional {@code SLOW_REQUEST} warning log line.
     * Override via {@code gateway.slow-request-threshold-ms} in application.yml.
     */
    @Value("${gateway.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    // ── Filter ────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String safeQuery = sanitiseQuery(request.getURI());
        String correlationId = resolveCorrelationId(request);
        String clientIp = resolveClientIp(request);

        // Monotonic timer — immune to NTP adjustments and clock skew
        long startNano = System.nanoTime();

        log.info("GATEWAY_IN  method={} path={} query={} clientIp={} correlationId={}",
                method, path, safeQuery, clientIp, correlationId);

        return chain.filter(exchange)
                // doFinally fires on completion, error, AND cancellation (e.g. client disconnect)
                .doFinally(signalType -> {
                    long durationMs = (System.nanoTime() - startNano) / 1_000_000L;
                    HttpStatus status = resolveStatus(exchange);

                    log.info("GATEWAY_OUT method={} path={} status={} durationMs={} signal={} correlationId={}",
                            method, path, status, durationMs, signalType, correlationId);

                    // Slow-request warning — separate line so it can be independently alerted in Kibana
                    if (durationMs > slowRequestThresholdMs) {
                        log.warn("SLOW_REQUEST method={} path={} durationMs={} thresholdMs={} correlationId={}",
                                method, path, durationMs, slowRequestThresholdMs, correlationId);
                    }

                    // Server-error warning — 5xx responses logged at WARN for alerting
                    if (status != null && status.is5xxServerError()) {
                        log.warn("SERVER_ERROR method={} path={} status={} durationMs={} correlationId={}",
                                method, path, status, durationMs, correlationId);
                    }

                    // Log client disconnects explicitly — they inflate p99 latency metrics
                    if (signalType == SignalType.CANCEL) {
                        log.warn("CLIENT_DISCONNECT method={} path={} durationMs={} correlationId={}",
                                method, path, durationMs, correlationId);
                    }
                });
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the correlation ID from the request headers.
     * Returns {@code "-"} (not null) if absent so log patterns remain consistent.
     */
    private String resolveCorrelationId(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst("X-Correlation-Id"))
                .filter(id -> !id.isBlank())
                .orElse("-");
    }

    /**
     * Resolves the real client IP, respecting {@code X-Forwarded-For} set by
     * upstream load balancers. Only the first (original) address is used.
     */
    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For: client, proxy1, proxy2  — take the leftmost
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("-");
    }

    /**
     * Returns the HTTP status from the response, or {@code null} if the
     * exchange completed via cancellation before a status was set.
     */
    private HttpStatus resolveStatus(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getResponse().getStatusCode())
                .filter(s -> s instanceof HttpStatus)
                .map(s -> (HttpStatus) s)
                .orElse(null);
    }

    /**
     * Returns the sanitised query string with sensitive parameter values
     * replaced by {@code [REDACTED]}, or {@code "-"} if there is no query string.
     *
     * <p>Example: {@code ?email=user@test.com&token=abc123}
     * becomes {@code ?email=user@test.com&token=[REDACTED]}
     */
    private String sanitiseQuery(URI uri) {
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        return SENSITIVE_PARAM_PATTERN.matcher(raw).replaceAll("$1=[REDACTED]");
    }

    /**
     * Builds a compiled regex that matches {@code key=value} for every name in
     * {@link #SENSITIVE_PARAMS}, case-insensitively.
     *
     * <p>Pattern: {@code (?i)(token|password|...)=([^&]*)}
     */
    private static Pattern buildSensitiveParamPattern() {
        String joined = String.join("|", SENSITIVE_PARAMS);
        return Pattern.compile("(?i)(" + joined + ")=([^&]*)");
    }
}