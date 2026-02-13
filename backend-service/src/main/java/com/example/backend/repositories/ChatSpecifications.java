package com.example.backend.repositories;

import com.example.backend.models.ChatMessage;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;

public class ChatSpecifications {

    public static Specification<ChatMessage> byUserId(String userId) {
        return (root, query, cb) ->
            userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<ChatMessage> createdAfter(Instant from) {
        return (root, query, cb) ->
            from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<ChatMessage> createdBefore(Instant to) {
        return (root, query, cb) ->
            to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
