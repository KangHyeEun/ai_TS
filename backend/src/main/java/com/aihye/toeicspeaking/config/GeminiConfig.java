package com.aihye.toeicspeaking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.keys}")
    private String apiKeys;

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
    public List<String> geminiApiKeys() {
        return Arrays.stream(apiKeys.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());
    }
}
