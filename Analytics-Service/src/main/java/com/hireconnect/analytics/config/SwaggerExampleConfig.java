package com.hireconnect.analytics.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SwaggerExampleConfig {

    private static final Map<String, Object> PARAMETER_EXAMPLES = Map.ofEntries(
            Map.entry("jobId", 101L),
            Map.entry("recruiterId", 12L),
            Map.entry("viewerId", 21L),
            Map.entry("viewerRole", "CANDIDATE")
    );

    @Bean
    public OperationCustomizer exampleParameterCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(this::applyExample);
            }
            return operation;
        };
    }

    private void applyExample(Parameter parameter) {
        if (parameter.getExample() == null && PARAMETER_EXAMPLES.containsKey(parameter.getName())) {
            parameter.setExample(PARAMETER_EXAMPLES.get(parameter.getName()));
        }
    }
}
