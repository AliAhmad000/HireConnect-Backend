package com.hireconnect.profile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.resume-dir:uploads/resumes}")
    private String resumeUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resumeLocation = Path.of(resumeUploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!resumeLocation.endsWith("/")) {
            resumeLocation += "/";
        }
        registry.addResourceHandler("/uploads/resumes/**")
                .addResourceLocations(resumeLocation);
    }
}
