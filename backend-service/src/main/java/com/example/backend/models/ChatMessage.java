package com.example.backend.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    // Constructors
    public ChatMessage() {
        this.createdAt = Instant.now();
    }

    public ChatMessage(String userId, String prompt, String response) {
        this.userId = userId;
        this.prompt = prompt;
        this.response = response;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}