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

    @Transactional(isolation = Isolation.READ_COMMITTED)  // TCL
    public ChatMessage createChat(String userEmail, String prompt) {
        // ACID in this method:
        // Atomicity: all or nothing
        // Consistency: foreign key + not null enforced
        // Isolation: READ_COMMITTED prevents dirty reads
        // Durability: WAL + PostgreSQL flush

        // BEGIN TRANSACTION (Spring handles)
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> userRepository.save(new User(userEmail, "User"))); // Assume name is "User" for simplicity

        ChatMessage msg = new ChatMessage(user, prompt, null);
        chatRepository.save(msg);           // DML
        String response = llmClient.infer(prompt);
        msg.setResponse(response);
        chatRepository.save(msg);           // DML
        return msg;                     // COMMIT
    }

    public Page<ChatMessage> getChats(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return Page.empty();
        Specification<ChatMessage> spec = ChatSpecifications.byUser(user);
        return chatRepository.findAll(spec, pageable);
    }

    public ChatMessage getChatById(Long id) {
        return chatRepository.findById(id).orElse(null);
    }

    public ChatMessage updateChat(Long id, String prompt, String response) {
        return chatRepository.findById(id).map(chat -> {
            chat.setPrompt(prompt);
            chat.setResponse(response);
            return chatRepository.save(chat);
        }).orElse(null);
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
