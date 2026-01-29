package com.example.backend.services;

import com.example.backend.dto.InferResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LLMClient {

    @Value("${openai.service-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public LLMClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String infer(String prompt) {
        Map<String, String> body = Map.of("prompt", prompt);

        ResponseEntity<InferResponse> res =
                restTemplate.postForEntity(
                        baseUrl + "/infer",
                        body,
                        InferResponse.class
                );

        InferResponse response = res.getBody();
        if (response == null || response.response() == null) {
            throw new IllegalStateException("Empty or invalid inference response");
        }

        return response.response();
    }
}
