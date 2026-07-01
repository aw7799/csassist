package com.csassist.service.ticket.web;

import com.csassist.service.ticket.IllegalTransitionException;
import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketAuditEntry;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        .content("{\"title\":\"Updated\"}"))
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

    @Test
    void changeStatusReturns200WhenLegal() throws Exception {
        when(ticketService.changeStatus(eq(1L), eq(TicketStatus.IN_PROGRESS), eq("agent:jsmith"), any()))
                .thenReturn(ticket(1L, "Printer broken", TicketStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\",\"changedBy\":\"agent:jsmith\",\"note\":\"starting work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatusReturns409WhenIllegal() throws Exception {
        when(ticketService.changeStatus(eq(1L), eq(TicketStatus.RESOLVED), eq("agent:jsmith"), any()))
                .thenThrow(new IllegalTransitionException(TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS)));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"RESOLVED\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("OPEN"))
                .andExpect(jsonPath("$.allowedNext[0]").value("IN_PROGRESS"));
    }

    @Test
    void changeStatusReturns400WhenChangedByMissing() throws Exception {
        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatusReturns400WhenStatusLiteralUnknown() throws Exception {
        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"BOGUS\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatusReturns404WhenMissing() throws Exception {
        when(ticketService.changeStatus(eq(99L), any(), any(), any()))
                .thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(patch("/api/tickets/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyReturns200WithEntries() throws Exception {
        TicketAuditEntry entry = new TicketAuditEntry();
        entry.setId(1L);
        entry.setTicketId(1L);
        entry.setFromStatus(TicketStatus.OPEN);
        entry.setToStatus(TicketStatus.IN_PROGRESS);
        entry.setChangedBy("agent:jsmith");
        entry.setChangedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(ticketService.history(1L)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/tickets/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].toStatus").value("IN_PROGRESS"));
    }

    @Test
    void historyReturns404WhenMissing() throws Exception {
        when(ticketService.history(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99/history"))
                .andExpect(status().isNotFound());
    }
}
