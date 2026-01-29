package com.example.backend.controllers;

import com.example.backend.models.ChatMessage;
import com.example.backend.services.ChatService;
import com.example.backend.services.LLMClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private LLMClient llm;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (!body.containsKey("prompt")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid input"));
        }

        String userId = (String) request.getAttribute("userId");
        String response = llm.infer(body.get("prompt"));

        chatService.save(userId, body.get("prompt"));
        chatService.save(userId, response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("response", response));
    }

    @GetMapping("/{userId}")
    public List<ChatMessage> history(@PathVariable String userId) {
        return chatService.history(userId);
    }
}
