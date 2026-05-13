package com.hireconnect.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregates all microservice Swagger UIs into a single Gateway Swagger UI.
 * Access at http://localhost:8080/swagger-ui.html → select service from dropdown.
 */
@Configuration
public class SwaggerAggregationConfig {

    private static final List<String> SERVICES = List.of(
            "auth-service",
            "profile-service",
            "job-service",
            "application-service",
            "interview-service",
            "notification-service",
            "subscription-service",
            "analytics-service"
    );

    @Bean
    @Lazy(false)
    public Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrls(
            SwaggerUiConfigProperties swaggerUiConfig) {
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        for (String service : SERVICES) {
            AbstractSwaggerUiConfigProperties.SwaggerUrl url =
                    new AbstractSwaggerUiConfigProperties.SwaggerUrl();
            url.setName(service);
            url.setUrl("/docs/" + service + "/api-docs");
            urls.add(url);
        }
        swaggerUiConfig.setUrls(urls);
        return urls;
    }
}
