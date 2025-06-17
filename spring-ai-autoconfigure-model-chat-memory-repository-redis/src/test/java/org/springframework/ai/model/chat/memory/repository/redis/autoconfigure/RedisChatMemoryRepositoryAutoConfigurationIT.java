package org.springframework.ai.model.chat.memory.repository.redis.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepositoryConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class RedisChatMemoryRepositoryAutoConfigurationIT {

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                    .withExposedPorts(6379);

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    RedisChatMemoryRepositoryAutoConfiguration.class,
                                    RedisAutoConfiguration.class))
                    .withPropertyValues(
                            String.format("spring.data.redis.host=%s", redisContainer.getHost()),
                            String.format(
                                    "spring.data.redis.port=%s",
                                    redisContainer.getFirstMappedPort()));

    @Test
    void useDefaultConfiguration() {
        this.contextRunner.run(
                context -> {
                    var chatMemoryRepository = context.getBean(RedisChatMemoryRepository.class);

                    var config = chatMemoryRepository.getConfig();
                    assertThat(config.getKeyPrefix())
                            .isEqualTo(RedisChatMemoryRepositoryConfig.DEFAULT_KEY_PREFIX);
                    assertThat(config.getTimeToLive()).isEqualTo(-1L);
                    assertThat(config.getRedisTemplate()).isInstanceOf(StringRedisTemplate.class);

                    var conversationId = UUID.randomUUID().toString();
                    assertThat(chatMemoryRepository.findByConversationId(conversationId)).isEmpty();

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

                    chatMemoryRepository.deleteByConversationId(conversationId);

                    var count = chatMemoryRepository.findByConversationId(conversationId);
                    assertThat(count.size()).isZero();
                });
    }

    @Test
    void setCustomConfiguration() {
        final String keyPrefix = "test_chat_memory:";
        final String timeToLive = "1h";

        this.contextRunner
                .withPropertyValues(
                        "spring.ai.chat.memory.repository.redis.key-prefix=" + keyPrefix)
                .withPropertyValues(
                        "spring.ai.chat.memory.repository.redis.time-to-live=" + timeToLive)
                .run(
                        context -> {
                            var chatMemoryRepository =
                                    context.getBean(RedisChatMemoryRepository.class);

                            var config = chatMemoryRepository.getConfig();
                            assertThat(config.getKeyPrefix()).isEqualTo(keyPrefix);
                            assertThat(config.getTimeToLive()).isEqualTo(3600L);
                            assertThat(config.getRedisTemplate())
                                    .isInstanceOf(StringRedisTemplate.class);

                            var conversationId = UUID.randomUUID().toString();
                            assertThat(chatMemoryRepository.findByConversationId(conversationId))
                                    .isEmpty();

                            var messages =
                                    List.<Message>of(
                                            new AssistantMessage(
                                                    "Message from assistant 1 - " + conversationId),
                                            new AssistantMessage(
                                                    "Message from assistant 2 - " + conversationId),
                                            new UserMessage(
                                                    "Message from user - " + conversationId),
                                            new SystemMessage(
                                                    "Message from system - " + conversationId));

                            chatMemoryRepository.saveAll(conversationId, messages);

                            assertThat(chatMemoryRepository.findConversationIds())
                                    .contains(conversationId);

                            var results = chatMemoryRepository.findByConversationId(conversationId);
                            assertThat(results).hasSize(messages.size());

                            // Verify the order is preserved (ascending by index)
                            for (var i = 0; i < messages.size(); i++) {
                                var message = messages.get(i);
                                var result = results.get(i);

                                assertThat(result.getMessageType())
                                        .isEqualTo(message.getMessageType());
                                assertThat(result.getText()).isEqualTo(message.getText());
                                assertThat(result.getMetadata()).containsKey("timestamp");
                            }

                            chatMemoryRepository.deleteByConversationId(conversationId);

                            var count = chatMemoryRepository.findByConversationId(conversationId);
                            assertThat(count.size()).isZero();
                        });
    }
}
