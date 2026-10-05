package com.starvoicelanka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public WebMvcConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static assets like style.css, app.js
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");

        // Performance media uploads
        Path uploadPath = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }
        registry.addResourceHandler("/media/**")
                .addResourceLocations(uploadUri);
    }
}
