package com.csassist.service.ticket.dto;

import com.csassist.service.ticket.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull TicketStatus toStatus,
        @NotBlank String changedBy,
        String note
) {
}
