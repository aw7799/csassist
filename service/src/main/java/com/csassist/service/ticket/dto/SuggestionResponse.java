package com.csassist.service.ticket.dto;

import java.time.Instant;

public record SuggestionResponse(
        Long id,
        Long ticketId,
        String articleId,
        String title,
        String category,
        String reason,
        Instant createdAt
) {
}
