package com.hireconnect.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            // Allow CORS preflight requests
            if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
                return chain.filter(exchange);
            }

           
            String path = request.getURI().getPath();

            // Skip JWT check for public paths
            if (isPublicPath(path, request.getMethod().name())) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String role   = claims.get("role", String.class);
                String email  = claims.get("email", String.class);
                String fullName = claims.get("fullName", String.class);

                // Propagate user info to downstream services via headers
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Name", fullName != null ? fullName : "")
                        .build();

                log.debug("JWT validated for userId={}, role={}, path={}", userId, role, path);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("Expired JWT token for path: {}", path);
                return unauthorizedResponse(exchange, "JWT token has expired");
            } catch (SignatureException e) {
                log.warn("Invalid JWT signature for path: {}", path);
                return unauthorizedResponse(exchange, "Invalid JWT signature");
            } catch (MalformedJwtException e) {
                log.warn("Malformed JWT token for path: {}", path);
                return unauthorizedResponse(exchange, "Malformed JWT token");
            } catch (Exception e) {
                log.error("JWT validation error for path {}: {}", path, e.getMessage());
                return unauthorizedResponse(exchange, "JWT validation failed");
            }
        };
    }

    private boolean isPublicPath(String path, String method) {
        boolean isGet = "GET".equalsIgnoreCase(method);

        return path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/forgot-password")
                || path.equals("/api/v1/auth/reset-password")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/validate")
                || path.startsWith("/api/v1/auth/oauth2/")
                || path.startsWith("/login/oauth2/")

                // Public job browsing only
                || (isGet && path.equals("/api/v1/jobs"))
                || (isGet && path.startsWith("/api/v1/jobs/search"))
                || (isGet && path.matches("^/api/v1/jobs/\\d+$"))

                || path.startsWith("/actuator")
                || path.startsWith("/docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars/");
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"message":"%s","statusCode":401}
                """.formatted(message);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Config properties can be added here if needed
    }
}
