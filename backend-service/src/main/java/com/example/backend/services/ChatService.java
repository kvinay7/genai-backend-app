package com.example.backend.services;

import com.example.backend.models.ChatMessage;
import com.example.backend.models.User;
import com.example.backend.repositories.ChatRepository;
import com.example.backend.repositories.ChatSpecifications;
import com.example.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LLMClient llmClient;

    public Page<ChatMessage> getChats(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return Page.empty();
        Specification<ChatMessage> spec = ChatSpecifications.byUser(user);
        return chatRepository.findAll(spec, pageable);
    }

    public ChatMessage getChatById(Long id) {
        return chatRepository.findById(id).orElse(null);
    }

    // Step 1: Save prompt and commit immediately — release DB connection
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessage savePendingChat(String userEmail, String prompt) {
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> userRepository.save(new User(userEmail, "User")));
        ChatMessage msg = new ChatMessage(user, prompt, null);
        return chatRepository.save(msg);
        // Transaction commits here — DB connection returned to pool
    }

    // Step 2: Save response in a separate transaction after LLM call
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessage updateChatResponse(Long chatId, String response) {
        ChatMessage msg = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        msg.setResponse(response);
        return chatRepository.save(msg);
        // Transaction commits here
    }

    // Orchestrator — no @Transactional here
    public ChatMessage createChat(String userEmail, String prompt) {
        ChatMessage msg = savePendingChat(userEmail, prompt);  // TX 1 — commits fast
        String response = llmClient.infer(prompt);             // LLM call — no DB lock
        return updateChatResponse(msg.getId(), response);      // TX 2 — commits fast
    }

    public void deleteChat(Long id) {
        chatRepository.deleteById(id);
    }

    public void deleteChatsByUserId(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user != null) {
            chatRepository.deleteAll(user.getChats());
        }
    }
}
