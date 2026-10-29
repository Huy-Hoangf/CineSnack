package com.cinesnack.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of("uploads", "posters").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/posters/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry boDinhTuyen) {
        boDinhTuyen.addRedirectViewController("/", "/html/index.html");
    }
}
