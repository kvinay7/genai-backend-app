package com.example.backend.services;

import com.example.backend.models.ChatMessage;
import com.example.backend.repositories.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatRepository repo;

    public void save(String userId, String msg) {
        ChatMessage c = new ChatMessage();
        c.setUserId(userId);
        c.setMessage(msg);
        c.setCreatedAt(Instant.now());
        repo.save(c);
    }

    public List<ChatMessage> history(String userId) {
        return repo.findByUserIdOrderByCreatedAt(userId);
    }
}
