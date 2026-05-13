package com.hireconnect.job.config;

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
            Map.entry("page", 0),
            Map.entry("size", 20),
            Map.entry("sortBy", "postedAt"),
            Map.entry("sortDir", "desc"),
            Map.entry("title", "Java Developer"),
            Map.entry("location", "Bengaluru"),
            Map.entry("category", "Software Development"),
            Map.entry("jobType", "FULL_TIME"),
            Map.entry("experience", 5),
            Map.entry("minSalary", 1200000),
            Map.entry("maxSalary", 1800000),
            Map.entry("status", "ACTIVE")
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
