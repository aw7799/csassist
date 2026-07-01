package com.csassist.service.ticket;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAuditEntryRepository auditEntryRepository;

    public TicketService(TicketRepository ticketRepository, TicketAuditEntryRepository auditEntryRepository) {
        this.ticketRepository = ticketRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    public List<Ticket> list() {
        return ticketRepository.findAll();
    }

    public Ticket getById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket create(Ticket ticket) {
        if (ticket.getStatus() == null) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        Instant now = Instant.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    public Ticket update(Long id, Ticket updates) {
        Ticket existing = getById(id);
        existing.setTitle(updates.getTitle());
        existing.setDescription(updates.getDescription());
        existing.setAssignee(updates.getAssignee());
        existing.setUpdatedAt(Instant.now());
        return ticketRepository.save(existing);
    }

    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        ticketRepository.deleteById(id);
    }

    @Transactional
    public Ticket changeStatus(Long id, TicketStatus toStatus, String changedBy, String note) {
        Ticket ticket = getById(id);
        TicketStatus current = ticket.getStatus();
        if (!current.canTransitionTo(toStatus)) {
            throw new IllegalTransitionException(current, current.allowedNext());
        }
        ticket.setStatus(toStatus);
        ticket.setUpdatedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        TicketAuditEntry entry = new TicketAuditEntry();
        entry.setTicketId(id);
        entry.setFromStatus(current);
        entry.setToStatus(toStatus);
        entry.setChangedBy(changedBy);
        entry.setNote(note);
        auditEntryRepository.save(entry);

        return saved;
    }

    public List<TicketAuditEntry> history(Long id) {
        getById(id);
        return auditEntryRepository.findByTicketIdOrderByChangedAtAscIdAsc(id);
    }
}
