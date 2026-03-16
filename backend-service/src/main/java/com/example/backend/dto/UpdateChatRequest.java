package com.example.backend.dto;

public record UpdateChatRequest(
        String prompt,
        String response
) {
}
