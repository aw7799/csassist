package com.csassist.service.ticket.web;

import com.csassist.service.ticket.Ticket;
import com.csassist.service.ticket.TicketService;
import com.csassist.service.ticket.dto.TicketRequest;
import com.csassist.service.ticket.mapper.TicketMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tickets")
public class TicketViewController {

    private final TicketService ticketService;

    public TicketViewController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tickets", ticketService.list());
        return "tickets/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("ticketRequest", new TicketRequest("", null, null, null));
        model.addAttribute("isEdit", false);
        return "tickets/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("ticketRequest") TicketRequest request, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "tickets/form";
        }
        ticketService.create(TicketMapper.toEntity(request));
        return "redirect:/tickets";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Ticket ticket = ticketService.getById(id);
        TicketRequest request = new TicketRequest(ticket.getTitle(), ticket.getDescription(), ticket.getStatus(), ticket.getAssignee());
        model.addAttribute("ticketRequest", request);
        model.addAttribute("isEdit", true);
        model.addAttribute("ticketId", id);
        return "tickets/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("ticketRequest") TicketRequest request,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("ticketId", id);
            return "tickets/form";
        }
        ticketService.update(id, TicketMapper.toEntity(request));
        return "redirect:/tickets";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ticketService.delete(id);
        return "redirect:/tickets";
    }
}
