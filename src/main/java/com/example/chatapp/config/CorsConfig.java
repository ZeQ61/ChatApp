package com.example.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // tüm endpoint'lere izin verir
                        .allowedOrigins("*") // tüm kaynaklara izin ver
                        .allowedMethods("*") // tüm HTTP metodlarına izin ver
                        .allowedHeaders("*") // tüm header'lara izin ver
                        .exposedHeaders("*") // tüm header'ları göster
                        .maxAge(3600); // önbellek süresi (saniye)
            }
        };
    }
}
