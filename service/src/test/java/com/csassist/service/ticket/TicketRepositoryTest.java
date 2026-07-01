package com.csassist.service.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void savesAndFindsTicketById() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Printer not working");
        ticket.setDescription("Jam on floor 3");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignee("alice");
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());

        Ticket saved = ticketRepository.save(ticket);

        assertThat(saved.getId()).isNotNull();

        Optional<Ticket> found = ticketRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Printer not working");
        assertThat(found.get().getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void findAllReturnsSavedTickets() {
        Ticket a = new Ticket();
        a.setTitle("Ticket A");
        a.setStatus(TicketStatus.OPEN);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        ticketRepository.save(a);

        Ticket b = new Ticket();
        b.setTitle("Ticket B");
        b.setStatus(TicketStatus.OPEN);
        b.setCreatedAt(Instant.now());
        b.setUpdatedAt(Instant.now());
        ticketRepository.save(b);

        List<Ticket> all = ticketRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteByIdRemovesTicket() {
        Ticket ticket = new Ticket();
        ticket.setTitle("To delete");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        ticketRepository.deleteById(saved.getId());

        assertThat(ticketRepository.findById(saved.getId())).isEmpty();
    }
}
