package com.ecommerce.apigateway.filter;

import com.ecommerce.apigateway.security.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * <p>Responsibilities (in execution order):
 * <ol>
 *   <li>Resolve or generate a correlation ID and propagate it downstream.</li>
 *   <li>Strip all trusted internal headers from the incoming request to prevent
 *       client header-injection attacks on both public and protected paths.</li>
 *   <li>Short-circuit public paths without JWT validation.</li>
 *   <li>Validate the Bearer JWT and forward verified identity headers
 *       (X-User-Id, X-User-Role, X-User-Email) to downstream services.</li>
 *   <li>Return structured JSON error responses on auth failures.</li>
 *   <li>Propagate the correlation ID into the reactive context for MDC bridging.</li>
 * </ol>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} + 1 so it executes before all
 * other filters but after Spring's built-in NettyWriteResponseFilter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;


    /**
     * Filter order — runs early, before route-level filters.
     */
    private static final int FILTER_ORDER = -100;

    /**
     * Internal trusted headers that downstream services accept as verified
     * identity claims. These MUST be stripped from every inbound request to
     * prevent clients from impersonating other users.
     */
    private static final List<String> INTERNAL_HEADERS = List.of(
            "X-User-Id",
            "X-User-Role",
            "X-User-Email",
            "X-User-FirstName"
    );

    /**
     * Paths exempt from JWT validation.
     * Supports Ant-style wildcards (e.g. {@code /api/v1/products/**}).
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/products",
            "/api/v1/products/**",

            "/actuator/**",
            "/fallback/**",

            "/v3/api-docs/**",
            "/*/v3/api-docs",
            "/*/v3/api-docs/**",

            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String path = exchange.getRequest().getURI().getPath();

        // ── Step 1: Resolve or generate correlation ID ────────────────────────
        final String correlationId = Optional
                .ofNullable(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());

        // ── Step 2: Sanitise — strip ALL trusted internal headers unconditionally.
        //    This must happen BEFORE the public-path short-circuit so even
        //    unauthenticated requests cannot inject identity headers.
        ServerHttpRequest sanitised = exchange.getRequest().mutate()
                .headers(h -> {
                    INTERNAL_HEADERS.forEach(h::remove);
                    h.set("X-Correlation-Id", correlationId);
                }).build();

        ServerWebExchange sanitisedExchange = exchange.mutate()
                .request(sanitised)
                .build();

        // ── Step 3: Short-circuit public paths ───────────────────────────────
        if (isPublicPath(path)) {
            log.debug("PUBLIC path={} correlationId={}", path, correlationId);
            return chain.filter(sanitisedExchange)
                    .contextWrite(buildContext(correlationId));
        }

        // ── Step 4: Extract Bearer token ─────────────────────────────────────
        String auth = sanitised.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("MISSING_TOKEN path={} correlationId={}", path, correlationId);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header",
                    path, correlationId);
        }

        String token = auth.substring(7);

        // ── Step 5: Parse and validate JWT ────────────────────────────────────
        try {
            Claims claims = jwtUtil.parse(token);

            // ── Step 6: Forward verified identity headers ─────────────────────
            //    Use set() not add() — guarantees exactly one value per header.
            ServerHttpRequest authenticated = sanitised.mutate()
                    .headers(h -> {
                        h.set("X-User-Id", claims.getSubject());
                        h.set("X-User-Role", getClaimSafely(claims, "role"));
                        h.set("X-User-Email", getClaimSafely(claims, "email"));
                        h.set("X-User-FirstName", getClaimSafely(claims, "firstName"));
                    })
                    .build();

            log.debug("JWT_VALID userId={} role={} path={} correlationId={}",
                    claims.getSubject(),
                    getClaimSafely(claims, "role"),
                    path,
                    correlationId);

            // ── Step 7: Propagate context for reactive MDC bridging ───────────
            return chain.filter(sanitisedExchange.mutate().request(authenticated).build())
                    .contextWrite(buildContext(correlationId, claims.getSubject()));

        } catch (ExpiredJwtException ex) {
            log.warn("JWT_EXPIRED path={} correlationId={} subject={} expiredAt={}",
                    path, correlationId,
                    ex.getClaims().getSubject(),
                    ex.getClaims().getExpiration());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token has expired",
                    path, correlationId);

        } catch (JwtException ex) {
            log.warn("JWT_INVALID path={} correlationId={} reason={}",
                    path, correlationId, ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token is invalid",
                    path, correlationId);

        } catch (Exception ex) {
            log.error("JWT_FILTER_ERROR path={} correlationId={}",
                    path, correlationId, ex);
            return reject(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Authentication processing error", path, correlationId);
        }
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }


    /**
     * Returns {@code true} if the path matches any entry in {@link #PUBLIC_PATHS}.
     * Supports both exact matches and Ant-style wildcard patterns.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * Builds a structured JSON 4xx/5xx response.
     * Uses Jackson serialisation so the JSON is always well-formed regardless
     * of special characters in the message.
     */
    private Mono<Void> reject(
            ServerWebExchange exchange,
            HttpStatus status,
            String message,
            String path,
            String correlationId) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Correlation-Id", correlationId);

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                correlationId
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            // Extremely unlikely — fallback to a hardcoded safe payload
            log.error("Failed to serialise error response", e);
            bytes = ("{\"error\":\"" + status.getReasonPhrase() + "\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * Safely extracts a String claim — returns an empty string rather than
     * throwing if the claim is absent or null.
     */
    private String getClaimSafely(Claims claims, String key) {
        return Optional.ofNullable(claims.get(key, String.class)).orElse("");
    }

    /**
     * Builds a reactive {@link Context} that carries the correlation ID (and
     * optionally the user ID) for MDC bridging via
     * {@code io.micrometer:context-propagation}.
     */
    private Context buildContext(String correlationId) {
        return Context.of("correlationId", correlationId);
    }

    private Context buildContext(String correlationId, String userId) {
        return Context.of(
                "correlationId", correlationId,
                "userId", userId
        );
    }


    /**
     * Structured JSON error body returned on authentication failures.
     *
     * <pre>
     * {
     *   "timestamp":     "2024-01-15T10:30:00Z",
     *   "status":        401,
     *   "error":         "Unauthorized",
     *   "message":       "Token has expired",
     *   "path":          "/api/v1/orders",
     *   "correlationId": "a1b2c3d4-..."
     * }
     * </pre>
     */
    private record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String path,
            String correlationId
    ) {
    }
}