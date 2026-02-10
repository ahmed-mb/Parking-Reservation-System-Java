package com.ahmedbahaj.parking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the React SPA from Spring Boot's static resources.
 * 
 * In production/demo mode, the React build output (dist/) is copied
 * into src/main/resources/static/ during the Docker build.
 * 
 * This config ensures that:
 * - /api/** requests go to controllers (handled by Spring MVC)
 * - Static assets (JS, CSS, images) are served directly
 * - All other routes return index.html (React Router handles them)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the resource exists (JS, CSS, images, etc.), serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Otherwise, return index.html for React Router
                        // (but not for API routes - those are handled by controllers)
                        if (!resourcePath.startsWith("api/") && !resourcePath.startsWith("actuator/")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}
