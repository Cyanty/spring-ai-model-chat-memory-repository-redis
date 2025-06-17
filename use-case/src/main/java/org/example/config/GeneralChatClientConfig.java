package org.example.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** general chat client config */
@Configuration
public class GeneralChatClientConfig {

    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    private final RedisChatMemoryRepository redisChatMemoryRepository;

    public GeneralChatClientConfig(
            JdbcChatMemoryRepository jdbcChatMemoryRepository,
            RedisChatMemoryRepository redisChatMemoryRepository) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.redisChatMemoryRepository = redisChatMemoryRepository;
    }

    @Bean(name = "messageWindowChatMemoryWithJdbc")
    public MessageWindowChatMemory messageWindowChatMemoryWithJdbc() {
        int maxMessages = 20;
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(
                        jdbcChatMemoryRepository) // default: new InMemoryChatMemoryRepository()
                .maxMessages(maxMessages)
                .build();
    }

    @Bean(name = "messageWindowChatMemoryWithRedis")
    public MessageWindowChatMemory messageWindowChatMemoryWithRedis() {
        int maxMessages = 20;
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(
                        redisChatMemoryRepository) // default: new InMemoryChatMemoryRepository()
                .maxMessages(maxMessages)
                .build();
    }

    @Bean(name = "deepseekV3ClientWithJdbc")
    public ChatClient deepseekV3ClientWithJdbc(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String modelName,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Qualifier("messageWindowChatMemoryWithJdbc") MessageWindowChatMemory messageWindowChatMemoryWithJdbc) {

        OpenAiApi build = OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();

        OpenAiChatModel openAiChatModel =
                OpenAiChatModel.builder()
                        .openAiApi(build)
                        .defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
                        .build();

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(messageWindowChatMemoryWithJdbc).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean(name = "deepseekV3ClientWithRedis")
    public ChatClient deepseekV3ClientWithRedis(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String modelName,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Qualifier("messageWindowChatMemoryWithRedis") MessageWindowChatMemory messageWindowChatMemoryWithRedis) {

        OpenAiApi build = OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();

        OpenAiChatModel openAiChatModel =
                OpenAiChatModel.builder()
                        .openAiApi(build)
                        .defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
                        .build();

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(messageWindowChatMemoryWithRedis).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
