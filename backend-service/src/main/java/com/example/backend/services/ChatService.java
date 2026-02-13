package com.example.backend.services;

import com.example.backend.models.ChatMessage;
import com.example.backend.repositories.ChatRepository;
import com.example.backend.repositories.ChatSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LLMClient llmClient;

    public ChatMessage createChat(String userId, String prompt) {
        String response = llmClient.infer(prompt);
        ChatMessage message = new ChatMessage(userId, prompt, response);
        message.setCreatedAt(Instant.now());
        return chatRepository.save(message);
    }

    public Page<ChatMessage> getChats(String userId, Pageable pageable) {
        Specification<ChatMessage> spec = ChatSpecifications.byUserId(userId);
        return chatRepository.findAll(spec, pageable);
    }

    public ChatMessage getChatById(Long id) {
        return chatRepository.findById(id).orElse(null);
    }

    public ChatMessage updateChat(Long id, String prompt, String response) {
        return chatRepository.findById(id).map(chat -> {
            chat.setPrompt(prompt);
            chat.setResponse(response);
            chat.setUpdatedAt(Instant.now());
            return chatRepository.save(chat);
        }).orElse(null);
    }

    public void deleteChat(Long id) {
        chatRepository.deleteById(id);
    }

    public void deleteChatsByUserId(String userId) {
        var chatsToDelete = chatRepository.findAll(ChatSpecifications.byUserId(userId));
        chatRepository.deleteAll(chatsToDelete);
    }
}
