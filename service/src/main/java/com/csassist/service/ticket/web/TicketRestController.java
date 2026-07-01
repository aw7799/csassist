package com.csassist.service.ticket.web;

import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketService;
import com.csassist.service.ticket.dto.TicketRequest;
import com.csassist.service.ticket.dto.TicketResponse;
import com.csassist.service.ticket.mapper.TicketMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketRequest request) {
        Ticket updated = ticketService.update(id, TicketMapper.toEntity(request));
        return TicketMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
