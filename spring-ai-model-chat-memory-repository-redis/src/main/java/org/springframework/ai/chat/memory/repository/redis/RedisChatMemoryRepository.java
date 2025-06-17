package org.springframework.ai.chat.memory.repository.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link ChatMemoryRepository} for Redis
 *
 * @author Cyanty
 * @since 1.0.0
 */
public final class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

    private final RedisTemplate<String, String> redisTemplate;

    private final RedisChatMemoryRepositoryConfig config;

    private final ObjectMapper objectMapper;

    private RedisChatMemoryRepository(RedisChatMemoryRepositoryConfig config) {
        this(config.getRedisTemplate(), config);
    }

    private RedisChatMemoryRepository(
            RedisTemplate<String, String> redisTemplate, RedisChatMemoryRepositoryConfig config) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public RedisChatMemoryRepositoryConfig getConfig() {
        return config;
    }

    @Override
    public List<String> findConversationIds() {
        return redisTemplate.execute(
                (RedisCallback<List<String>>)
                        connection -> {
                            var keys = new HashSet<String>();
                            ScanOptions options =
                                    ScanOptions.scanOptions()
                                            .match(String.format("*%s*", config.getKeyPrefix()))
                                            .count(Integer.MAX_VALUE)
                                            .build();

                            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                                while (cursor.hasNext()) {
                                    String[] key =
                                            new String(cursor.next(), StandardCharsets.UTF_8)
                                                    .split(":");
                                    if (key.length > 0) {
                                        keys.add(key[key.length - 1]);
                                    }
                                }
                            }
                            return new ArrayList<>(keys);
                        });
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        String key = config.getKeyPrefix() + conversationId;
        List<String> messageStrings = redisTemplate.opsForList().range(key, 0, -1);
        if (messageStrings == null) {
            logger.debug("No messages found for conversationId: " + conversationId);
            return List.of();
        }

        List<Message> messages = new ArrayList<>();
        for (String messageString : messageStrings) {
            try {
                JsonNode jsonNode = objectMapper.readTree(messageString);
                messages.add(getMessage(jsonNode));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing message", e);
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        String key = config.getKeyPrefix() + conversationId;

        deleteByConversationId(conversationId);

        List<String> messageJsons =
                messages.stream()
                        .map(
                                message -> {
                                    try {
                                        message.getMetadata()
                                                .put("timestamp", Instant.now().toString());
                                        return objectMapper.writeValueAsString(message);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException("Error serializing message", e);
                                    }
                                })
                        .toList();

        redisTemplate.opsForList().rightPushAll(key, messageJsons);
        if (config.getTimeToLive() > 0) {
            redisTemplate.expire(key, config.getTimeToLive(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        String key = config.getKeyPrefix() + conversationId;
        redisTemplate.delete(key);
    }

    private Message getMessage(JsonNode jsonNode) {
        String type =
                Optional.ofNullable(jsonNode)
                        .map(node -> node.get("messageType"))
                        .map(JsonNode::asText)
                        .orElse(MessageType.USER.getValue());
        MessageType messageType = MessageType.valueOf(type.toUpperCase());

        String textContent =
                Optional.ofNullable(jsonNode)
                        .map(node -> node.get("text"))
                        .map(JsonNode::asText)
                        .orElseGet(
                                () ->
                                        (messageType == MessageType.SYSTEM
                                                        || messageType == MessageType.USER)
                                                ? ""
                                                : null);

        Map<String, Object> metadata =
                Optional.ofNullable(jsonNode)
                        .map(node -> node.get("metadata"))
                        .map(
                                node ->
                                        objectMapper.convertValue(
                                                node, new TypeReference<Map<String, Object>>() {}))
                        .orElse(new HashMap<>());
        metadata.put("timestamp", Instant.now().toString());

        return switch (messageType) {
            case ASSISTANT -> new AssistantMessage(textContent, metadata);
            case USER -> UserMessage.builder().text(textContent).metadata(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(textContent).metadata(metadata).build();
            case TOOL -> new ToolResponseMessage(List.of(), metadata);
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    /** RedisChatMemoryRepository Builder */
    public static final class Builder {
        private final RedisChatMemoryRepositoryConfig.Builder builder =
                RedisChatMemoryRepositoryConfig.builder();

        private Builder() {}

        public Builder keyPrefix(String keyPrefix) {
            this.builder.withKeyPrefix(keyPrefix);
            return this;
        }

        public Builder timeToLive(String timeToLive) {
            this.builder.withTimeToLive(timeToLive);
            return this;
        }

        public Builder redisTemplate(RedisTemplate<String, String> redisTemplate) {
            this.builder.withRedisTemplate(redisTemplate);
            return this;
        }

        public RedisChatMemoryRepository build() {
            return new RedisChatMemoryRepository(this.builder.build());
        }
    }
}
