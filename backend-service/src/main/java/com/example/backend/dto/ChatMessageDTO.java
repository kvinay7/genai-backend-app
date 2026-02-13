package com.example.backend.dto;

import java.time.Instant;

public record ChatMessageDTO(
    Long id,
    String userId,
    String prompt,
    String response,
    Instant createdAt,
    Instant updatedAt
) {}
