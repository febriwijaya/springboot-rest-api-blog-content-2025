package com.content.springboot_rest_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Semua request ke /photos/** akan diarahkan ke folder uploads/photos/
        registry.addResourceHandler("/photos/**")
                .addResourceLocations("file:uploads/photos/")
                .setCachePeriod(3600); // cache 1 jam biar lebih cepat
    }
}
