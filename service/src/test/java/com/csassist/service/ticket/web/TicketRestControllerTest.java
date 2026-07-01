package com.csassist.service.ticket.web;

import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketNotFoundException;
import com.csassist.service.ticket.TicketService;
import com.csassist.service.ticket.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketRestController.class)
class TicketRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private Ticket ticket(Long id, String title, TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTitle(title);
        t.setDescription("desc");
        t.setStatus(status);
        t.setAssignee("alice");
        t.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        t.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return t;
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(ticketService.create(any(Ticket.class))).thenReturn(ticket(1L, "Printer broken", TicketStatus.OPEN));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Printer broken\",\"description\":\"desc\",\"assignee\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tickets/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createReturns400WhenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturns200WithTickets() throws Exception {
        when(ticketService.list()).thenReturn(List.of(ticket(1L, "A", TicketStatus.OPEN), ticket(2L, "B", TicketStatus.CLOSED)));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        when(ticketService.getById(1L)).thenReturn(ticket(1L, "Found", TicketStatus.OPEN));

        mockMvc.perform(get("/api/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Found"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(ticketService.getById(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 99 not found"));
    }

    @Test
    void updateReturns200WhenFound() throws Exception {
        when(ticketService.update(eq(1L), any(Ticket.class))).thenReturn(ticket(1L, "Updated", TicketStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(ticketService.update(eq(99L), any(Ticket.class))).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(put("/api/tickets/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns400WhenTitleBlank() throws Exception {
        mockMvc.perform(put("/api/tickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204WhenFound() throws Exception {
        mockMvc.perform(delete("/api/tickets/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new TicketNotFoundException(99L)).when(ticketService).delete(99L);

        mockMvc.perform(delete("/api/tickets/99"))
                .andExpect(status().isNotFound());
    }
}
