package com.csassist.service.ticket.web;

import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketService;
import com.csassist.service.ticket.dto.AuditEntryResponse;
import com.csassist.service.ticket.dto.StatusChangeRequest;
import com.csassist.service.ticket.dto.TicketRequest;
import com.csassist.service.ticket.dto.TicketResponse;
import com.csassist.service.ticket.dto.TicketUpdateRequest;
import com.csassist.service.ticket.mapper.TicketMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketRestController {

    private final TicketService ticketService;

    public TicketRestController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        Ticket created = ticketService.create(TicketMapper.toEntity(request));
        return ResponseEntity.created(URI.create("/api/tickets/" + created.getId()))
                .body(TicketMapper.toResponse(created));
    }

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.list().stream().map(TicketMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return TicketMapper.toResponse(ticketService.getById(id));
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        Ticket updated = ticketService.update(id, TicketMapper.toEntity(request));
        return TicketMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        Ticket updated = ticketService.changeStatus(id, request.toStatus(), request.changedBy(), request.note());
        return TicketMapper.toResponse(updated);
    }

    @GetMapping("/{id}/history")
    public List<AuditEntryResponse> history(@PathVariable Long id) {
        return ticketService.history(id).stream().map(TicketMapper::toAuditResponse).toList();
    }
}
