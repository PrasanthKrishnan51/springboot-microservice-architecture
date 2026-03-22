package com.ecommerce.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT utility for the API Gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validates and initialises the signing key at startup — fails fast if
 *       the secret is absent or too short, so a misconfigured deployment never
 *       silently accepts tokens.</li>
 *   <li>Parses and validates incoming JWTs: signature, expiry, issuer, and
 *       (optionally) audience.</li>
 *   <li>Exposes {@link #parse(String)} only — callers handle the exception
 *       types they care about ({@link ExpiredJwtException}, {@link JwtException})
 *       rather than a boolean wrapper that loses context.</li>
 * </ul>
 *
 * <p><strong>Key rotation:</strong> the signing key is resolved once at startup
 * from {@code jwt.secret}. Rotating the key requires a rolling restart of the
 * gateway. Hot-reload is not supported by this implementation.
 *
 * <p><strong>Thread safety:</strong> this class is stateless after construction
 * and safe for concurrent use.
 */
@Component
@Slf4j
public class JwtUtil {

    /**
     * Minimum byte length for the HMAC-SHA-256 key as required by RFC 7518 §3.2.
     * Keys shorter than 256 bits (32 bytes) are rejected.
     */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * The signing key is marked {@code transient} to prevent accidental
     * serialisation into caches, sessions, or distributed stores.
     */
    private final transient SecretKey signingKey;

    /**
     * Expected {@code iss} claim value. Empty string disables issuer validation.
     */
    private final String expectedIssuer;

    /**
     * Expected {@code aud} claim value. Empty string disables audience validation.
     */
    private final String expectedAudience;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructs a {@code JwtUtil} and eagerly validates the secret.
     *
     * @param secret           the raw HMAC-SHA-256 secret from configuration
     * @param expectedIssuer   the required {@code iss} claim (empty = skip check)
     * @param expectedAudience the required {@code aud} claim (empty = skip check)
     * @throws IllegalStateException if the secret is blank or too short
     * @throws WeakKeyException      if JJWT rejects the key for cryptographic reasons
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expected-issuer:}") String expectedIssuer,
            @Value("${jwt.expected-audience:}") String expectedAudience) {


        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "jwt.secret must not be blank. Set it via an environment variable or secret manager.");
        }

        // Measure in bytes (not chars) — matches what Keys.hmacShaKeyFor() receives.
        // Multi-byte UTF-8 chars make char-count an unreliable proxy for key strength.
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(String.format(
                    "jwt.secret is too short: %d bytes provided, minimum is %d bytes (%d-bit key). "
                            + "Generate a secure value with: openssl rand -base64 32",
                    secretBytes.length, MIN_SECRET_BYTES, MIN_SECRET_BYTES * 8));
        }

        // JJWT performs its own key-strength validation here and throws WeakKeyException
        // if the key does not meet HMAC-SHA-256 requirements — let it propagate so the
        // application fails at startup rather than silently accepting weak tokens.
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);

        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;

        log.info("JwtUtil initialised keyLengthBytes={} issuerCheck={} audienceCheck={}",
                secretBytes.length,
                StringUtils.hasText(expectedIssuer),
                StringUtils.hasText(expectedAudience));
    }


    /**
     * Parses and fully validates a compact JWT string.
     *
     * <p>Validates:
     * <ul>
     *   <li>HMAC-SHA-256 signature against the configured key</li>
     *   <li>Expiry ({@code exp} claim)</li>
     *   <li>{@code iss} claim — if {@code jwt.expected-issuer} is configured</li>
     *   <li>{@code aud} claim — if {@code jwt.expected-audience} is configured</li>
     * </ul>
     *
     * @param token the compact JWT string (without "Bearer " prefix)
     * @return the verified {@link Claims} payload
     * @throws ExpiredJwtException if the token's {@code exp} claim is in the past
     * @throws JwtException        for any other validation failure (bad signature,
     *                             malformed token, wrong issuer/audience, etc.)
     */
    public Claims parse(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtException("Token must not be blank");
        }

        try {
            var parserBuilder = Jwts.parser()
                    .verifyWith(signingKey);

            // Issuer validation — rejects tokens signed by a different service
            // even if they share the same secret
            if (StringUtils.hasText(expectedIssuer)) {
                parserBuilder.requireIssuer(expectedIssuer);
            }

            // Audience validation — rejects tokens not intended for this gateway
            if (StringUtils.hasText(expectedAudience)) {
                parserBuilder.requireAudience(expectedAudience);
            }

            Claims claims = parserBuilder
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("JWT_PARSED sub={} iss={} exp={}",
                    claims.getSubject(),
                    claims.getIssuer(),
                    claims.getExpiration());

            return claims;

        } catch (ExpiredJwtException ex) {
            // Re-throw as-is — callers distinguish expired tokens from invalid ones
            // to return appropriate HTTP responses and metrics.
            log.debug("JWT_EXPIRED sub={} expiredAt={}",
                    ex.getClaims().getSubject(),
                    ex.getClaims().getExpiration());
            throw ex;

        } catch (JwtException ex) {
            // Log type + message at debug — JJWT never includes the raw token
            // in exception messages, so this is safe to log.
            log.debug("JWT_INVALID type={} reason={}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}