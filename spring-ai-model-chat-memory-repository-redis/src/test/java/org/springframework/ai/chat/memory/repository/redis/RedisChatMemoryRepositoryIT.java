package org.springframework.ai.chat.memory.repository.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/*
@TestPropertySource(properties = { "spring.data.redis.host=localhost" })
@ContextConfiguration(classes = RedisChatMemoryRepositoryIT.TestConfiguration.class)
 */
@Testcontainers
@SuppressWarnings("unchecked")
public class RedisChatMemoryRepositoryIT {

    private static final String TEST_REDIS_TEMPLATE = "redisTemplate";

    private static final String TEST_STRING_REDIS_TEMPLATE = "stringRedisTemplate";

    private static final String TEST_CHAT_MEMORY_KEY_PREFIX = "test_chat_memory:";

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                    .withExposedPorts(6379);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    // .withUserConfiguration(RedisChatMemoryRepositoryIT.TestConfiguration.class);
                    .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                    .withPropertyValues(
                            String.format("spring.data.redis.host=%s", redisContainer.getHost()),
                            String.format(
                                    "spring.data.redis.port=%s",
                                    redisContainer.getFirstMappedPort()));

    @Test
    void verifyBeansAndContextInitialization() {
        this.contextRunner.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(
                                    org.springframework.data.redis.connection.RedisConnectionFactory
                                            .class);
                    assertThat(context).hasBean(TEST_REDIS_TEMPLATE);
                    assertThat(context).hasBean(TEST_STRING_REDIS_TEMPLATE);
                });
    }

    @ParameterizedTest
    @CsvSource({
        "Message from assistant,ASSISTANT",
        "Message from user,USER",
        "Message from system,SYSTEM",
        "Message from tool,TOOL"
    })
    void saveMessagesSingleMessage(String content, MessageType messageType) {
        List.of(TEST_REDIS_TEMPLATE, TEST_STRING_REDIS_TEMPLATE)
                .forEach(
                        template ->
                                this.contextRunner.run(
                                        context -> {
                                            var redisTemplate =
                                                    context.getBean(template, RedisTemplate.class);
                                            var chatMemoryRepository =
                                                    getRedisChatMemoryRepository(redisTemplate);

                                            var conversationId = UUID.randomUUID().toString();
                                            var message =
                                                    switch (messageType) {
                                                        case ASSISTANT -> new AssistantMessage(
                                                                content);
                                                        case USER -> new UserMessage(content);
                                                        case SYSTEM -> new SystemMessage(content);
                                                        case TOOL -> new ToolResponseMessage(
                                                                List.of());
                                                    };

                                            chatMemoryRepository.saveAll(
                                                    conversationId, List.of(message));

                                            assertThat(chatMemoryRepository.findConversationIds())
                                                    .contains(conversationId);

                                            Long expire =
                                                    redisTemplate.getExpire(
                                                            TEST_CHAT_MEMORY_KEY_PREFIX
                                                                    + conversationId);
                                            assertThat(expire).isNotEqualTo(-1L);

                                            var results =
                                                    chatMemoryRepository.findByConversationId(
                                                            conversationId);
                                            assertThat(results.get(0).getMessageType())
                                                    .isEqualTo(message.getMessageType());
                                            // TOOL messages don't have text
                                            if (messageType != MessageType.TOOL) {
                                                assertThat(results.get(0).getText())
                                                        .isEqualTo(message.getText());
                                            }
                                            assertThat(results.get(0).getMetadata())
                                                    .containsKey("timestamp");
                                        }));
    }

    @ParameterizedTest
    @CsvSource({TEST_REDIS_TEMPLATE, TEST_STRING_REDIS_TEMPLATE})
    void saveMessagesMultipleMessages(String template) {
        this.contextRunner.run(
                context -> {
                    var redisTemplate = context.getBean(template, RedisTemplate.class);
                    var chatMemoryRepository = getRedisChatMemoryRepository(redisTemplate);

                    var conversationId = UUID.randomUUID().toString();
                    var messages =
                            List.<Message>of(
                                    new AssistantMessage(
                                            "Message from assistant 1 - " + conversationId),
                                    new AssistantMessage(
                                            "Message from assistant 2 - " + conversationId),
                                    new UserMessage("Message from user - " + conversationId),
                                    new SystemMessage("Message from system - " + conversationId));

                    chatMemoryRepository.saveAll(conversationId, messages);

                    assertThat(chatMemoryRepository.findConversationIds()).contains(conversationId);

                    Long expire =
                            redisTemplate.getExpire(TEST_CHAT_MEMORY_KEY_PREFIX + conversationId);
                    assertThat(expire).isNotEqualTo(-1L);

                    var results = chatMemoryRepository.findByConversationId(conversationId);
                    assertThat(results).hasSize(messages.size());

                    // Verify the order is preserved (ascending by index)
                    for (var i = 0; i < messages.size(); i++) {
                        var message = messages.get(i);
                        var result = results.get(i);

                        assertThat(result.getMessageType()).isEqualTo(message.getMessageType());
                        assertThat(result.getText()).isEqualTo(message.getText());
                        assertThat(result.getMetadata()).containsKey("timestamp");
                    }
                });
    }

    @ParameterizedTest
    @CsvSource({TEST_REDIS_TEMPLATE, TEST_STRING_REDIS_TEMPLATE})
    void findMessagesByConversationId(String template) {
        this.contextRunner.run(
                context -> {
                    var redisTemplate = context.getBean(template, RedisTemplate.class);
                    RedisChatMemoryRepository redisChatMemoryRepository =
                            getRedisChatMemoryRepository(redisTemplate);

                    var conversationId = UUID.randomUUID().toString();
                    var messages =
                            List.<Message>of(
                                    new AssistantMessage(
                                            "Message from assistant 1 - " + conversationId),
                                    new AssistantMessage(
                                            "Message from assistant 2 - " + conversationId),
                                    new UserMessage("Message from user - " + conversationId),
                                    new SystemMessage("Message from system - " + conversationId));

                    redisChatMemoryRepository.saveAll(conversationId, messages);

                    var results = redisChatMemoryRepository.findByConversationId(conversationId);
                    assertThat(results.size()).isEqualTo(messages.size());

                    for (var i = 0; i < messages.size(); i++) {
                        var message = messages.get(i);
                        var result = results.get(i);

                        assertThat(result.getMessageType()).isEqualTo(message.getMessageType());
                        assertThat(result.getText()).isEqualTo(message.getText());
                        assertThat(result.getMetadata()).containsKey("timestamp");
                    }
                });
    }

    @ParameterizedTest
    @CsvSource({TEST_REDIS_TEMPLATE, TEST_STRING_REDIS_TEMPLATE})
    void deleteMessagesByConversationId(String template) {
        this.contextRunner.run(
                context -> {
                    var redisTemplate = context.getBean(template, RedisTemplate.class);
                    RedisChatMemoryRepository chatMemoryRepository =
                            getRedisChatMemoryRepository(redisTemplate);

                    var conversationId = UUID.randomUUID().toString();
                    var messages =
                            List.<Message>of(
                                    new AssistantMessage(
                                            "Message from assistant 1 - " + conversationId),
                                    new AssistantMessage(
                                            "Message from assistant 2 - " + conversationId),
                                    new UserMessage("Message from user - " + conversationId),
                                    new SystemMessage("Message from system - " + conversationId));

                    chatMemoryRepository.saveAll(conversationId, messages);

                    chatMemoryRepository.deleteByConversationId(conversationId);

                    var count =
                            redisTemplate
                                    .opsForList()
                                    .range(TEST_CHAT_MEMORY_KEY_PREFIX + conversationId, 0, -1);
                    assertThat(count.size()).isZero();
                });
    }

    private RedisChatMemoryRepository getRedisChatMemoryRepository(
            RedisTemplate<String, String> redisTemplate) {
        return RedisChatMemoryRepository.builder()
                .keyPrefix(TEST_CHAT_MEMORY_KEY_PREFIX)
                .timeToLive("30m")
                .redisTemplate(redisTemplate)
                .build();
    }

    /*  // Base configuration for all integration tests.
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = { RedisAutoConfiguration.class })
    static class TestConfiguration {

        @Bean
        public RedisChatMemoryRepository redisChatMemoryRepository() {
            return RedisChatMemoryRepository.builder().redisTemplate(redisTemplate).build();
        }

        @Bean
        public RedisChatMemoryRepositoryConfig redisChatMemoryRepositoryConfig() {
            return RedisChatMemoryRepositoryConfig.builder()
                    .withKeyPrefix("test_chat_memory:")
                    .withTimeToLive("-1")
                    .withRedisTemplate(redisTemplate)
                    .build();
        }
    }
     */

}
