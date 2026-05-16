package com.hireconnect.profile.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;
    
    @PostConstruct
    public void init() {
        System.out.println("========== CLOUDINARY CONFIG INIT ==========");
    }
    
    @Bean
    public CommandLineRunner runner() {
        return args -> {
            System.out.println("========== NEW BUILD RUNNING ==========");
        };
    }
    
    

    @Bean
    public Cloudinary cloudinary() {
    	System.out.println("CLOUDINARY CONFIG LOADED");
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
    
    
}