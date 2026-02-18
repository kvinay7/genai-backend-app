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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatMessage> createChat(
            HttpServletRequest request,
            @Valid @RequestBody CreateChatRequest body) {

        String userId = (String) request.getAttribute("userId");
        ChatMessage response = chatService.createChat(userId, body.prompt());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ChatMessage>> getChats(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        String userId = (String) request.getAttribute("userId");
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ChatMessage> chats = chatService.getChats(userId, pageable);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatMessage> getChat(@PathVariable Long id) {
        ChatMessage chat = chatService.getChatById(id);
        if (chat != null) {
            return ResponseEntity.ok(chat);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatMessage> updateChat(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChatRequest request) {
        ChatMessage updated = chatService.updateChat(id, request.prompt(), request.response());
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        chatService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteChatsByUser(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        chatService.deleteChatsByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
