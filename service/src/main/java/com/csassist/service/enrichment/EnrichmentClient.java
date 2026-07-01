package com.csassist.service.enrichment;

import java.util.List;

public interface EnrichmentClient {

    List<SuggestedArticle> suggestArticles(String ticketTitle, String ticketDescription);
}
