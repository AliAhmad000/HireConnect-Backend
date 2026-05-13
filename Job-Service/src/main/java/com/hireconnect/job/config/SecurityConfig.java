package com.hireconnect.job.config;

import com.hireconnect.job.config.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — browsing/searching jobs needs no login
                .requestMatchers(HttpMethod.GET, "/api/v1/jobs",
                                               "/api/v1/jobs/search").permitAll()
                .requestMatchers(
                        new RegexRequestMatcher("^/api/v1/jobs/\\d+$", "GET"),
                        new RegexRequestMatcher("^/api/v1/jobs/\\d+/exists$", "GET"),
                        new RegexRequestMatcher("^/api/v1/jobs/\\d+/recruiter$", "GET"),
                        new RegexRequestMatcher("^/api/v1/jobs/recruiter/\\d+/count$", "GET")
                ).permitAll()
                .requestMatchers("/actuator/**",
                                 "/api-docs/**",
                                 "/swagger-ui/**",
                                 "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
