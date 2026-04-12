package com.example.ronproject.memo;

import java.time.Instant;
import java.util.UUID;

public record MemoResponse(
        UUID id,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
