package com.example.backend.controllers;

import com.example.backend.models.ChatMessage;
import com.example.backend.services.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@CrossOrigin(origins = "*")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatMessage> createChat(
            @RequestParam String userId,
            @RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        ChatMessage response = chatService.createChat(userId, prompt);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ChatMessage>> getChats(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
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
            @RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String response = payload.get("response");
        ChatMessage updated = chatService.updateChat(id, prompt, response);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ChatMessage> patchChat(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        ChatMessage existing = chatService.getChatById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (payload.containsKey("prompt")) {
            existing.setPrompt(payload.get("prompt"));
        }
        if (payload.containsKey("response")) {
            existing.setResponse(payload.get("response"));
        }
        existing.setUpdatedAt(Instant.now());
        ChatMessage updated = chatService.updateChat(id, existing.getPrompt(), existing.getResponse());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long id) {
        chatService.deleteChat(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteChatsByUser(@RequestParam String userId) {
        chatService.deleteChatsByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
