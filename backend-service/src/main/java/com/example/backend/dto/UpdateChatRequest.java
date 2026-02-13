package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChatRequest(
    @NotBlank(message = "prompt is required")
    String prompt,
    
    @NotBlank(message = "response is required")
    String response
) {}
