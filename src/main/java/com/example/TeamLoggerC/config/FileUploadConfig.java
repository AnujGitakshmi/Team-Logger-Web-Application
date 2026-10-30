package com.example.TeamLoggerC.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        String uploadPath =
                Paths.get("uploads", "profile")
                        .toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString();

        registry.addResourceHandler(
                "/uploads/profile/**"
        ).addResourceLocations(
                uploadPath
        );
    }
}

