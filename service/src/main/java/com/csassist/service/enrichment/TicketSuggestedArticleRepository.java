package com.csassist.service.enrichment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSuggestedArticleRepository extends JpaRepository<TicketSuggestedArticle, Long> {

    List<TicketSuggestedArticle> findByTicketIdOrderByIdAsc(Long ticketId);
}
