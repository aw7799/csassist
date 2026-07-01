package com.csassist.service.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketUpdateRequest(
        @NotBlank String title,
        String description,
        String assignee
) {
}
