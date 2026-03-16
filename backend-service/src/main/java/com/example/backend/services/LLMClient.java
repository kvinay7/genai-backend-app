package com.example.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class LLMClient {

    @Value("${openai.service-url}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    public String infer(String prompt, Long userId, String email, String role, String requestId) {
        Map<String, String> body = Map.of("prompt", prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", requestId);
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Email", email);
        headers.set("X-User-Role", role);

        ResponseEntity<Map> res = restTemplate.postForEntity(
                baseUrl + "/infer",
                new HttpEntity<>(body, headers),
                Map.class
        );

        return res.getBody().get("response").toString();
    }

    @Async
    public CompletableFuture<String> inferAsync(String prompt) {
        return CompletableFuture.completedFuture(infer(prompt, -1L, "unknown", "USER", "async"));
    }
}
