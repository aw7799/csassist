package com.csassist.service.ticket.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class TicketViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRendersTicketsView() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"))
                .andExpect(model().attributeExists("tickets"));
    }

    @Test
    void newFormRendersFormView() throws Exception {
        mockMvc.perform(get("/tickets/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/form"));
    }

    @Test
    void createWithValidDataRedirectsAndPersists() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Wifi down")
                        .param("description", "Conference room B")
                        .param("assignee", "bob"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        mockMvc.perform(get("/tickets"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Wifi down")));
    }

    @Test
    void createWithBlankTitleRerendersFormWithErrors() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/form"))
                .andExpect(model().attributeHasFieldErrors("ticketRequest", "title"));
    }

    @Test
    void editFormRendersPrefilledForm() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Edit target")
                        .param("assignee", "carol"));

        String listBody = mockMvc.perform(get("/tickets"))
                .andReturn().getResponse().getContentAsString();
        Long id = extractFirstTicketId(listBody, "Edit target");

        mockMvc.perform(get("/tickets/" + id + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit target")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"status\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OPEN")));
    }

    @Test
    void updateRedirectsAndPersistsChangesButNeverTouchesStatus() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Update target")
                        .param("assignee", "dave"));

        String listBody = mockMvc.perform(get("/tickets"))
                .andReturn().getResponse().getContentAsString();
        Long id = extractFirstTicketId(listBody, "Update target");

        // A stray "status" param (as if a client tried to bypass the transition policy) must be ignored.
        mockMvc.perform(post("/tickets/" + id)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Updated title")
                        .param("status", "IN_PROGRESS")
                        .param("assignee", "dave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        String updatedListBody = mockMvc.perform(get("/tickets"))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(updatedListBody).contains("Updated title");
        org.assertj.core.api.Assertions.assertThat(extractStatusFor(updatedListBody, "Updated title")).isEqualTo("OPEN");
    }

    @Test
    void deleteRedirectsAndRemovesTicket() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", "Delete target")
                        .param("assignee", "erin"));

        String listBody = mockMvc.perform(get("/tickets"))
                .andReturn().getResponse().getContentAsString();
        Long id = extractFirstTicketId(listBody, "Delete target");

        mockMvc.perform(post("/tickets/" + id + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        mockMvc.perform(get("/tickets"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Delete target"))));
    }

    private Long extractFirstTicketId(String html, String titleMarker) {
        int titleIndex = html.indexOf(titleMarker);
        String before = html.substring(0, titleIndex);
        int idStart = before.lastIndexOf("data-ticket-id=\"") + "data-ticket-id=\"".length();
        int idEnd = before.indexOf('"', idStart);
        return Long.valueOf(before.substring(idStart, idEnd));
    }

    private String extractStatusFor(String html, String titleMarker) {
        String titleCell = ">" + titleMarker + "</td>";
        int titleCellIndex = html.indexOf(titleCell);
        int statusOpenTag = html.indexOf("<td>", titleCellIndex + titleCell.length());
        int statusTextStart = statusOpenTag + "<td>".length();
        int statusTextEnd = html.indexOf("</td>", statusTextStart);
        return html.substring(statusTextStart, statusTextEnd);
    }
}
