package com.csassist.service.ticket.web;

import com.csassist.service.enrichment.EnrichmentClient;
import com.csassist.service.enrichment.SuggestedArticle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrichmentClient enrichmentClient;

    private String createTicket(String title) throws Exception {
        return mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"assignee\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void createGetUpdateDeleteHappyPath() throws Exception {
        String location = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Printer not working\",\"description\":\"Jam on floor 3\",\"assignee\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Printer not working"));

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Printer not working\",\"assignee\":\"alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullLifecycleTransitionsProduceThreeOrderedAuditEntries() throws Exception {
        String location = createTicket("Full lifecycle ticket");

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"RESOLVED\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"CLOSED\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(location + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].fromStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].toStatus").value("RESOLVED"))
                .andExpect(jsonPath("$[2].fromStatus").value("RESOLVED"))
                .andExpect(jsonPath("$[2].toStatus").value("CLOSED"));
    }

    @Test
    void illegalTransitionReturns409AndWritesNoAuditEntry() throws Exception {
        String location = createTicket("Illegal transition ticket");

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"RESOLVED\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("OPEN"))
                .andExpect(jsonPath("$.allowedNext[0]").value("IN_PROGRESS"));

        mockMvc.perform(get(location))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mockMvc.perform(get(location + "/history"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void selfTransitionReturns409() throws Exception {
        String location = createTicket("Self transition ticket");

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"OPEN\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reopenClosedAllowedButInProgressToClosedRejected() throws Exception {
        String location = createTicket("Reopen ticket");

        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStatus\":\"IN_PROGRESS\",\"changedBy\":\"agent:jsmith\"}")).andExpect(status().isOk());

        // IN_PROGRESS -> CLOSED must be rejected (must resolve first)
        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStatus\":\"CLOSED\",\"changedBy\":\"agent:jsmith\"}")).andExpect(status().isConflict());

        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStatus\":\"RESOLVED\",\"changedBy\":\"agent:jsmith\"}")).andExpect(status().isOk());
        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStatus\":\"CLOSED\",\"changedBy\":\"agent:jsmith\"}")).andExpect(status().isOk());

        // reopen CLOSED -> IN_PROGRESS allowed
        mockMvc.perform(patch(location + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void missingChangedByReturns400() throws Exception {
        String location = createTicket("Missing changedBy ticket");

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownStatusLiteralReturns400() throws Exception {
        String location = createTicket("Unknown literal ticket");

        mockMvc.perform(patch(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"BOGUS\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transitionAndHistoryForNonexistentTicketReturn404() throws Exception {
        mockMvc.perform(patch("/api/tickets/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"OPEN\",\"changedBy\":\"agent:jsmith\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tickets/999999/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPersistsSuggestionsAndSuggestionsEndpointReturnsThem() throws Exception {
        when(enrichmentClient.suggestArticles(anyString(), any())).thenReturn(List.of(
                new SuggestedArticle("password-reset", "Reset your password", "account", "matches ticket about login issues")
        ));

        String location = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Cannot log in\",\"description\":\"forgot password\",\"assignee\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location + "/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].articleId").value("password-reset"))
                .andExpect(jsonPath("$[0].title").value("Reset your password"))
                .andExpect(jsonPath("$[0].category").value("account"));
    }

    @Test
    void createSucceedsAndSuggestionsEmptyWhenEnrichmentClientThrows() throws Exception {
        when(enrichmentClient.suggestArticles(anyString(), any())).thenThrow(new RuntimeException("llm down"));

        String location = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Enrichment failure ticket\",\"assignee\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location + "/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
