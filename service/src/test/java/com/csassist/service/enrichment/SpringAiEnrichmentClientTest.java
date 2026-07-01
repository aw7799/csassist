package com.csassist.service.enrichment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiEnrichmentClientTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ToolCallbackProvider toolCallbackProvider;

    private SpringAiEnrichmentClient enrichmentClient;

    @BeforeEach
    void setUp() {
        enrichmentClient = new SpringAiEnrichmentClient(chatClient, toolCallbackProvider);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any())).thenReturn(requestSpec);
    }

    @Test
    void suggestArticlesParsesWellFormedJsonIntoSuggestedArticles() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("""
                {"suggestions":[{"articleId":"password-reset","title":"Reset your password","category":"account","reason":"matches login issue"}]}
                """);

        List<SuggestedArticle> result = enrichmentClient.suggestArticles("Cannot log in", "forgot password");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).articleId()).isEqualTo("password-reset");
        assertThat(result.get(0).title()).isEqualTo("Reset your password");
        assertThat(result.get(0).category()).isEqualTo("account");
        assertThat(result.get(0).reason()).isEqualTo("matches login issue");
    }

    @Test
    void suggestArticlesReturnsEmptyListWhenModelReturnsMalformedJson() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("not json at all");

        List<SuggestedArticle> result = enrichmentClient.suggestArticles("Printer jam", "Paper stuck");

        assertThat(result).isEmpty();
    }

    @Test
    void suggestArticlesReturnsEmptyListWhenSuggestionsFieldMissing() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{}");

        List<SuggestedArticle> result = enrichmentClient.suggestArticles("Printer jam", "Paper stuck");

        assertThat(result).isEmpty();
    }

    @Test
    void suggestArticlesPropagatesExceptionWhenChatClientThrows() {
        when(requestSpec.call()).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> enrichmentClient.suggestArticles("Printer jam", "Paper stuck"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("network down");
    }
}
