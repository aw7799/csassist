package com.csassist.service.enrichment;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpringAiEnrichmentClient implements EnrichmentClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiEnrichmentClient.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public SpringAiEnrichmentClient(ChatClient enrichmentChatClient, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = enrichmentChatClient;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @Override
    public List<SuggestedArticle> suggestArticles(String ticketTitle, String ticketDescription) {
        BeanOutputConverter<EnrichmentSuggestions> converter = new BeanOutputConverter<>(EnrichmentSuggestions.class);

        String prompt = """
                A support ticket was created:
                Title: %s
                Description: %s

                Use the search_kb and get_article tools to find at most 3 relevant knowledge-base
                articles. Only include articles you actually retrieved via the tools -- never invent
                an articleId. If nothing relevant is found, return an empty suggestions list.

                %s
                """.formatted(ticketTitle, ticketDescription == null ? "" : ticketDescription, converter.getFormat());

        String raw = chatClient.prompt()
                .user(prompt)
                .tools(toolCallbackProvider)
                .call()
                .content();

        try {
            EnrichmentSuggestions parsed = converter.convert(raw);
            return parsed == null || parsed.suggestions() == null ? List.of() : parsed.suggestions();
        } catch (Exception ex) {
            log.warn("Failed to parse enrichment suggestions from LLM response: {}", ex.getMessage());
            return List.of();
        }
    }
}
