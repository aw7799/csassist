package com.csassist.service.ticket.dto;

import com.csassist.service.ticket.TicketStatus;

import java.time.Instant;

public record AuditEntryResponse(
        Long id,
        Long ticketId,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String changedBy,
        Instant changedAt,
        String note
) {
}
