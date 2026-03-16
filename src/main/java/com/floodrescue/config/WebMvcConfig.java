package com.floodrescue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static resource configuration for serving uploaded files.
 *
 * Maps URL paths under /uploads/** to the local "uploads" directory
 * in the project root. For example:
 *   URL  : /uploads/rescue/abc.jpeg
 *   File : ./uploads/rescue/abc.jpeg
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve all /uploads/** URLs from the local uploads directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(0);
    }
}

