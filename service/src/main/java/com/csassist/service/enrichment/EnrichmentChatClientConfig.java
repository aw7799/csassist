package com.csassist.service.enrichment;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnrichmentChatClientConfig {

    @Bean
    public ChatClient enrichmentChatClient(OllamaChatModel ollamaChatModel, OpenAiChatModel openAiChatModel,
                                            @Value("${csassist.enrichment.llm-provider:ollama}") String llmProvider) {
        ChatModel chatModel = "groq".equalsIgnoreCase(llmProvider) ? openAiChatModel : ollamaChatModel;
        return ChatClient.builder(chatModel).build();
    }
}
