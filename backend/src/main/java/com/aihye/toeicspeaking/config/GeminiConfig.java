package com.aihye.toeicspeaking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    @Bean
    public WebClient geminiWebClient() {
        String baseUrl = String.format(
            "https://generativelanguage.googleapis.com/v1beta/models/%s", model);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public String geminiApiKey() {
        return apiKey;
    }
}
