package com.example.backend.services;

import com.example.backend.exceptions.ForbiddenException;
import com.example.backend.exceptions.ResourceNotFoundException;
import com.example.backend.models.ChatMessage;
import com.example.backend.models.User;
import com.example.backend.repositories.ChatRepository;
import com.example.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private ChatMessage getOwnedChat(Long chatId, Long requestingUserId) {
        ChatMessage chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));

        if (!chat.getUser().getId().equals(requestingUserId)) {
            throw new ForbiddenException("Access denied");
        }

        return chat;
    }

    public Page<ChatMessage> getChatsForUser(Long userId, Pageable pageable) {
        return chatRepository.findByUserId(userId, pageable);
    }

    public ChatMessage getChat(Long chatId, Long userId) {
        return getOwnedChat(chatId, userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessage savePendingChat(Long userId, String prompt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ChatMessage msg = new ChatMessage(user, prompt, null);
        return chatRepository.save(msg);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessage updateChatResponse(Long chatId, String response) {
        ChatMessage msg = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
        msg.setResponse(response);
        return chatRepository.save(msg);
    }

    public ChatMessage createChat(Long userId, String userEmail, String role, String requestId, String prompt) {
        ChatMessage msg = savePendingChat(userId, prompt);
        String response = llmClient.infer(prompt, userId, userEmail, role, requestId);
        return updateChatResponse(msg.getId(), response);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ChatMessage updateChat(Long chatId, Long userId, String prompt, String response) {
        ChatMessage chat = getOwnedChat(chatId, userId);
        if (prompt != null && !prompt.isBlank()) {
            chat.setPrompt(prompt);
        }
        if (response != null && !response.isBlank()) {
            chat.setResponse(response);
        }
        return chatRepository.save(chat);
    }

    @Transactional
    public void deleteChat(Long chatId, Long userId, String role) {
        if ("ADMIN".equals(role)) {
            chatRepository.deleteById(chatId);
            return;
        }

        chatRepository.delete(getOwnedChat(chatId, userId));
    }

    @Transactional
    public void deleteAllChatsForUser(Long requestedUserId, Long requestingUserId, String role) {
        if (!requestedUserId.equals(requestingUserId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Access denied");
        }

        chatRepository.deleteByUserId(requestedUserId);
    }
}
