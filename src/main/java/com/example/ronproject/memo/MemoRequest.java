package com.example.ronproject.memo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String content
) {
}
