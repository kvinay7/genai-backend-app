package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChatRequest(
    @NotBlank(message = "prompt is required")
    String prompt
) {}
