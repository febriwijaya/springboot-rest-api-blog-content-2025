package com.content.springboot_rest_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.user-photo-dir}")
    private String userUploadDir;

    @Value("${app.upload.article-photo-dir}")
    private String articleUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose user photos
        registry.addResourceHandler("/uploads/photos/users/**")
                .addResourceLocations("file:" + Paths.get(userUploadDir).toAbsolutePath().toUri())
                .setCachePeriod(3600);

        // Expose article thumbnails
        registry.addResourceHandler("/uploads/photos/thumbnails/**")
                .addResourceLocations("file:" + Paths.get(articleUploadDir).toAbsolutePath().toUri())
                .setCachePeriod(3600);
    }
}
