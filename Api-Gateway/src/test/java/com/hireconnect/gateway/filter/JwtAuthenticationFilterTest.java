package com.hireconnect.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "HireConnect2024SecretKeyForJWTTokenValidationMustBe256BitsLong";

    private JwtAuthenticationFilter filterFactory;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filterFactory, "jwtSecret", SECRET);
    }

    @Test
    @DisplayName("should allow public GET job detail without token")
    void shouldAllowPublicGetJobDetailWithoutToken() {
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/jobs/42").build());

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("should reject protected PUT job update without token")
    void shouldRejectProtectedPutJobUpdateWithoutToken() {
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/v1/jobs/42").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verifyNoInteractions(chain);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should propagate user headers for valid token")
    void shouldPropagateUserHeadersForValidToken() {
        GatewayFilter filter = filterFactory.apply(new JwtAuthenticationFilter.Config());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        String token = tokenFor("7", "RECRUITER", "recruiter@example.com", "Asha Recruiter");

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.PUT, "/api/v1/jobs/42")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());

        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            forwardedExchange.set(invocation.getArgument(0));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ServerWebExchange mutated = forwardedExchange.get();
        assertThat(mutated).isNotNull();
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("7");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("RECRUITER");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("recruiter@example.com");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Name")).isEqualTo("Asha Recruiter");
    }

    private String tokenFor(String subject, String role, String email, String fullName) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .claim("email", email)
                .claim("fullName", fullName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
