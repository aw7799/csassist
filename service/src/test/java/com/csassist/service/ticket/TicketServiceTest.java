package com.csassist.service.ticket;

import com.csassist.service.enrichment.EnrichmentClient;
import com.csassist.service.enrichment.SuggestedArticle;
import com.csassist.service.enrichment.TicketSuggestedArticle;
import com.csassist.service.enrichment.TicketSuggestedArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketAuditEntryRepository auditEntryRepository;

    @Mock
    private EnrichmentClient enrichmentClient;

    @Mock
    private TicketSuggestedArticleRepository suggestedArticleRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, auditEntryRepository, enrichmentClient, suggestedArticleRepository);
        lenient().when(enrichmentClient.suggestArticles(anyString(), nullable(String.class))).thenReturn(List.of());
    }

    @Test
    void createDefaultsStatusToOpenAndSetsTimestampsWhenStatusMissing() {
        Ticket ticket = new Ticket();
        ticket.setTitle("New ticket");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket created = ticketService.create(ticket);

        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void createKeepsExplicitStatusWhenProvided() {
        Ticket ticket = new Ticket();
        ticket.setTitle("New ticket");
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket created = ticketService.create(ticket);

        assertThat(created.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void getByIdReturnsTicketWhenFound() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Existing");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Ticket found = ticketService.getById(1L);

        assertThat(found.getId()).isEqualTo(1L);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getById(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void listReturnsAllTickets() {
        when(ticketRepository.findAll()).thenReturn(List.of(new Ticket(), new Ticket()));

        List<Ticket> tickets = ticketService.list();

        assertThat(tickets).hasSize(2);
    }

    @Test
    void updateOverwritesFieldsButNeverTouchesStatus() {
        Ticket existing = new Ticket();
        existing.setId(1L);
        existing.setTitle("Old title");
        existing.setStatus(TicketStatus.OPEN);
        existing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        existing.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket updates = new Ticket();
        updates.setTitle("New title");
        updates.setDescription("New description");
        updates.setStatus(TicketStatus.IN_PROGRESS);
        updates.setAssignee("bob");

        Ticket updated = ticketService.update(1L, updates);

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(updated.getAssignee()).isEqualTo("bob");
        assertThat(updated.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(updated.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void updateThrowsWhenMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.update(99L, new Ticket()))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingTicket() {
        when(ticketRepository.existsById(1L)).thenReturn(true);

        ticketService.delete(1L);

        verify(ticketRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(ticketRepository.existsById(anyLong())).thenReturn(false);

        assertThatThrownBy(() -> ticketService.delete(99L))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository, never()).deleteById(any());
    }

    @Test
    void changeStatusAppliesLegalTransitionAndWritesOneAuditEntry() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket updated = ticketService.changeStatus(1L, TicketStatus.IN_PROGRESS, "agent:jsmith", "starting work");

        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        ArgumentCaptor<TicketAuditEntry> captor = ArgumentCaptor.forClass(TicketAuditEntry.class);
        verify(auditEntryRepository, times(1)).save(captor.capture());
        TicketAuditEntry entry = captor.getValue();
        assertThat(entry.getTicketId()).isEqualTo(1L);
        assertThat(entry.getFromStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(entry.getToStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(entry.getChangedBy()).isEqualTo("agent:jsmith");
        assertThat(entry.getNote()).isEqualTo("starting work");
    }

    @Test
    void changeStatusRejectsIllegalTransitionAndWritesNothing() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.changeStatus(1L, TicketStatus.RESOLVED, "agent:jsmith", null))
                .isInstanceOf(IllegalTransitionException.class)
                .satisfies(ex -> {
                    IllegalTransitionException ite = (IllegalTransitionException) ex;
                    assertThat(ite.getCurrentStatus()).isEqualTo(TicketStatus.OPEN);
                    assertThat(ite.getAllowedNext()).containsExactly(TicketStatus.IN_PROGRESS);
                });

        verify(ticketRepository, never()).save(any());
        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void changeStatusRejectsSelfTransition() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.changeStatus(1L, TicketStatus.OPEN, "agent:jsmith", null))
                .isInstanceOf(IllegalTransitionException.class);

        verify(ticketRepository, never()).save(any());
        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void changeStatusThrowsWhenTicketMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.changeStatus(99L, TicketStatus.IN_PROGRESS, "agent:jsmith", null))
                .isInstanceOf(TicketNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void historyReturnsEntriesForExistingTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(auditEntryRepository.findByTicketIdOrderByChangedAtAscIdAsc(1L))
                .thenReturn(List.of(new TicketAuditEntry(), new TicketAuditEntry()));

        List<TicketAuditEntry> history = ticketService.history(1L);

        assertThat(history).hasSize(2);
    }

    @Test
    void historyThrowsWhenTicketMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.history(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void createCallsEnrichmentClientWithTitleAndDescription() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Printer jam");
        ticket.setDescription("Paper stuck on floor 3");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.create(ticket);

        verify(enrichmentClient, times(1)).suggestArticles("Printer jam", "Paper stuck on floor 3");
    }

    @Test
    void createPersistsReturnedSuggestionsAgainstNewTicketId() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Cannot log in");
        ticket.setDescription("User forgot password");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(enrichmentClient.suggestArticles("Cannot log in", "User forgot password")).thenReturn(List.of(
                new SuggestedArticle("password-reset", "Reset your password", "account", "matches login issue"),
                new SuggestedArticle("account-locked", "Unlock your account", "account", "matches login issue")
        ));

        ticketService.create(ticket);

        ArgumentCaptor<TicketSuggestedArticle> captor = ArgumentCaptor.forClass(TicketSuggestedArticle.class);
        verify(suggestedArticleRepository, times(2)).save(captor.capture());
        List<TicketSuggestedArticle> saved = captor.getAllValues();
        assertThat(saved.get(0).getTicketId()).isEqualTo(42L);
        assertThat(saved.get(0).getArticleId()).isEqualTo("password-reset");
        assertThat(saved.get(0).getTitle()).isEqualTo("Reset your password");
        assertThat(saved.get(0).getCategory()).isEqualTo("account");
        assertThat(saved.get(0).getReason()).isEqualTo("matches login issue");
        assertThat(saved.get(1).getTicketId()).isEqualTo(42L);
        assertThat(saved.get(1).getArticleId()).isEqualTo("account-locked");
    }

    @Test
    void createSucceedsAndWritesNoSuggestionsWhenEnrichmentClientThrows() {
        Ticket ticket = new Ticket();
        ticket.setTitle("New ticket");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrichmentClient.suggestArticles(anyString(), nullable(String.class)))
                .thenThrow(new RuntimeException("llm down"));

        Ticket created = ticketService.create(ticket);

        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getCreatedAt()).isNotNull();
        verify(suggestedArticleRepository, never()).save(any());
    }

    @Test
    void createWritesNoSuggestionsWhenEnrichmentReturnsEmptyList() {
        Ticket ticket = new Ticket();
        ticket.setTitle("New ticket");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.create(ticket);

        verify(suggestedArticleRepository, never()).save(any());
    }

    @Test
    void suggestionsReturnsEntriesForExistingTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(suggestedArticleRepository.findByTicketIdOrderByIdAsc(1L))
                .thenReturn(List.of(new TicketSuggestedArticle(), new TicketSuggestedArticle()));

        List<TicketSuggestedArticle> suggestions = ticketService.suggestions(1L);

        assertThat(suggestions).hasSize(2);
    }

    @Test
    void suggestionsThrowsWhenTicketMissing() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.suggestions(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }
}
