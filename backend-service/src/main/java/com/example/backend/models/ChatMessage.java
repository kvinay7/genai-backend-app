package com.example.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Why PostgreSQL (DBMS) and not CSV/File System?
// - Persistence across restarts
// - ACID transactions for "save prompt + LLM response"
// - Relationships (User → many ChatMessages)
// - Indexing & querying (fast history fetch)

@Entity
@Table(name = "chat_messages",
       indexes = @Index(columnList = "user_id, created_at DESC")) // Composite index
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                    // Foreign Key + Referential Integrity

    @Column(nullable = false)
    private String prompt;

    private String response;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "user_name")
    private String userName;   // Denormalized for performance

    // Constructors
    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(User user, String prompt, String response) {
        this.user = user;
        this.prompt = prompt;
        this.response = response;
        this.userName = user.getName(); // Set denormalized field
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}