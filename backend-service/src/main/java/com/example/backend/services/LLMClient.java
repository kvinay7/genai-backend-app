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

    @Autowired
    private RestTemplate restTemplate;

    public String infer(String prompt) {
        Map<String, String> body = Map.of("prompt", prompt);

        ResponseEntity<Map> res =
            restTemplate.postForEntity(
                baseUrl + "/infer",
                body,
                Map.class
            );

        return res.getBody().get("response").toString();
    }
}
