package com.example.backend.controllers;

import com.example.backend.dto.CreateChatRequest;
import com.example.backend.dto.UpdateChatRequest;
import com.example.backend.models.ChatMessage;
import com.example.backend.services.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatMessage> createChat(
            HttpServletRequest request,
            @Valid @RequestBody CreateChatRequest body
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        String role = (String) request.getAttribute("role");
        String requestId = (String) request.getAttribute("requestId");

        ChatMessage response = chatService.createChat(userId, email, role, requestId, body.prompt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ChatMessage>> getChats(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Long userId = (Long) request.getAttribute("userId");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ChatMessage> chats = chatService.getChatsForUser(userId, pageable);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatMessage> getChat(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(chatService.getChat(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatMessage> updateChat(
            @PathVariable Long id,
            @RequestBody UpdateChatRequest body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(chatService.updateChat(id, userId, body.prompt(), body.response()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        chatService.deleteChat(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteChatsByUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        chatService.deleteAllChatsForUser(userId, userId, role);
        return ResponseEntity.noContent().build();
    }
}
