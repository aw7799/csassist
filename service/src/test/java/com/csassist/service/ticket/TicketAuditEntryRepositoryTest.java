package com.csassist.service.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketAuditEntryRepositoryTest {

    @Autowired
    private TicketAuditEntryRepository auditEntryRepository;

    private TicketAuditEntry entry(Long ticketId, TicketStatus from, TicketStatus to, Instant changedAt) {
        TicketAuditEntry entry = new TicketAuditEntry();
        entry.setTicketId(ticketId);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setChangedBy("agent:jsmith");
        entry.setChangedAt(changedAt);
        return entry;
    }

    @Test
    void findByTicketIdReturnsOnlyThatTicketsEntriesInChronologicalOrder() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T00:05:00Z");
        Instant t3 = Instant.parse("2026-01-01T00:10:00Z");

        auditEntryRepository.save(entry(1L, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, t2));
        auditEntryRepository.save(entry(1L, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, t3));
        auditEntryRepository.save(entry(2L, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, t1));
        auditEntryRepository.save(entry(1L, TicketStatus.CLOSED, TicketStatus.IN_PROGRESS, t1));

        List<TicketAuditEntry> ticket1Entries = auditEntryRepository.findByTicketIdOrderByChangedAtAscIdAsc(1L);

        assertThat(ticket1Entries).hasSize(3);
        assertThat(ticket1Entries).allMatch(e -> e.getTicketId().equals(1L));
        assertThat(ticket1Entries.get(0).getChangedAt()).isEqualTo(t1);
        assertThat(ticket1Entries.get(1).getChangedAt()).isEqualTo(t2);
        assertThat(ticket1Entries.get(2).getChangedAt()).isEqualTo(t3);
    }

    @Test
    void savedEntryHasGeneratedIdAndChangedAt() {
        TicketAuditEntry saved = auditEntryRepository.save(
                entry(5L, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChangedAt()).isNotNull();
    }
}
