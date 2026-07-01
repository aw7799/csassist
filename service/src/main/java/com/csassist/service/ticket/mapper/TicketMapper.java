package com.csassist.service.ticket.mapper;

import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.dto.TicketRequest;
import com.csassist.service.ticket.dto.TicketResponse;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static Ticket toEntity(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(request.status());
        ticket.setAssignee(request.assignee());
        return ticket;
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
