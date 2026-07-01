package com.csassist.service.ticket.mapper;

import com.csassist.service.enrichment.TicketSuggestedArticle;
import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketAuditEntry;
import com.csassist.service.ticket.dto.AuditEntryResponse;
import com.csassist.service.ticket.dto.SuggestionResponse;
import com.csassist.service.ticket.dto.TicketRequest;
import com.csassist.service.ticket.dto.TicketResponse;
import com.csassist.service.ticket.dto.TicketUpdateRequest;

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

    public static Ticket toEntity(TicketUpdateRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
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

    public static AuditEntryResponse toAuditResponse(TicketAuditEntry entry) {
        return new AuditEntryResponse(
                entry.getId(),
                entry.getTicketId(),
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getChangedBy(),
                entry.getChangedAt(),
                entry.getNote()
        );
    }

    public static SuggestionResponse toSuggestionResponse(TicketSuggestedArticle suggestion) {
        return new SuggestionResponse(
                suggestion.getId(),
                suggestion.getTicketId(),
                suggestion.getArticleId(),
                suggestion.getTitle(),
                suggestion.getCategory(),
                suggestion.getReason(),
                suggestion.getCreatedAt()
        );
    }
}
