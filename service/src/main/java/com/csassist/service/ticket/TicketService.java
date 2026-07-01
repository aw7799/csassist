package com.csassist.service.ticket;

import com.csassist.service.enrichment.EnrichmentClient;
import com.csassist.service.enrichment.SuggestedArticle;
import com.csassist.service.enrichment.TicketSuggestedArticle;
import com.csassist.service.enrichment.TicketSuggestedArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final TicketAuditEntryRepository auditEntryRepository;
    private final EnrichmentClient enrichmentClient;
    private final TicketSuggestedArticleRepository suggestedArticleRepository;

    public TicketService(TicketRepository ticketRepository, TicketAuditEntryRepository auditEntryRepository,
                          EnrichmentClient enrichmentClient, TicketSuggestedArticleRepository suggestedArticleRepository) {
        this.ticketRepository = ticketRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.enrichmentClient = enrichmentClient;
        this.suggestedArticleRepository = suggestedArticleRepository;
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
        Ticket saved = ticketRepository.save(ticket);
        enrich(saved);
        return saved;
    }

    private void enrich(Ticket ticket) {
        try {
            List<SuggestedArticle> suggestions = enrichmentClient.suggestArticles(ticket.getTitle(), ticket.getDescription());
            for (SuggestedArticle suggestion : suggestions) {
                TicketSuggestedArticle entity = new TicketSuggestedArticle();
                entity.setTicketId(ticket.getId());
                entity.setArticleId(suggestion.articleId());
                entity.setTitle(suggestion.title());
                entity.setCategory(suggestion.category());
                entity.setReason(suggestion.reason());
                suggestedArticleRepository.save(entity);
            }
        } catch (Exception ex) {
            log.warn("Enrichment failed for ticket {}: {}", ticket.getId(), ex.getMessage(), ex);
        }
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

    public List<TicketSuggestedArticle> suggestions(Long id) {
        getById(id);
        return suggestedArticleRepository.findByTicketIdOrderByIdAsc(id);
    }
}
