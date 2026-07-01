package com.csassist.service.ticket.dto;

import com.csassist.service.ticket.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {
}
