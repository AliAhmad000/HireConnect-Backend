package com.hireconnect.auth.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SwaggerExampleConfig {

    private static final Map<String, Object> PARAMETER_EXAMPLES = Map.ofEntries(
            Map.entry("userId", 21L),
            Map.entry("page", 0),
            Map.entry("size", 20),
            Map.entry("sortBy", "createdAt"),
            Map.entry("sortDir", "desc"),
            Map.entry("role", "RECRUITER"),
            Map.entry("status", "ACTIVE"),
            Map.entry("email", "user@hireconnect.com")
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
