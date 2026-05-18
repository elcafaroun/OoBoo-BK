package io.c4us.masterbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 🔹 Configuration du CORS global (React <-> Spring Boot)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // ✅ Autorise toutes les origines dynamiquement
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * 🔹 Configuration pour servir les fichiers uploadés depuis le dossier local
     * "uploads/"
     */

    

    @Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String path = "file:" + System.getProperty("user.home") + "/Downloads/Uploads/";
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations(path);

            
}
}
