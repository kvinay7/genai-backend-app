package com.example.backend.repositories;

import com.example.backend.models.ChatMessage;
import com.example.backend.models.User;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class ChatSpecifications {

    public static Specification<ChatMessage> byUser(User user) {
        return (root, query, cb) ->
            user == null ? null : cb.equal(root.get("user"), user);
    }

    public static Specification<ChatMessage> createdAfter(LocalDateTime from) {
        return (root, query, cb) ->
            from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<ChatMessage> createdBefore(LocalDateTime to) {
        return (root, query, cb) ->
            to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
