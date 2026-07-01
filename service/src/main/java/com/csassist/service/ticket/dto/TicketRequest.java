package com.csassist.service.ticket.dto;

import com.csassist.service.ticket.TicketStatus;
import jakarta.validation.constraints.NotBlank;

public record TicketRequest(
        @NotBlank String title,
        String description,
        TicketStatus status,
        String assignee
) {
}
