package com.csassist.service.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
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
    void updateOverwritesFieldsAndBumpsUpdatedAt() {
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
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
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
}
