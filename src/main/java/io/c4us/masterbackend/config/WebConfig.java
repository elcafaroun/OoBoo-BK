package io.c4us.masterbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Sert les fichiers uploadés depuis le dossier local de l'utilisateur
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chemin vers le dossier Uploads
        String path = "file:" + System.getProperty("user.home") + "/Downloads/Uploads/";
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path);
    }
}