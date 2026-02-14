package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRequest(
    String userId,
    @NotBlank(message = "prompt is required")
    String prompt
) {}
