package com.doinb.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Paths.get(uploadPath).toAbsolutePath().normalize().toString().replace("\\", "/");
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir);
    }
}
