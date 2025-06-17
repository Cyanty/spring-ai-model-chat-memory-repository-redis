package org.springframework.ai.model.chat.memory.repository.redis.autoconfigure;

import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepositoryConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auto-configuration for {@link RedisChatMemoryRepositoryConfig}. */
@ConfigurationProperties(RedisChatMemoryRepositoryProperties.CONFIG_PREFIX)
public class RedisChatMemoryRepositoryProperties {

    public static final String CONFIG_PREFIX = "spring.ai.chat.memory.repository.redis";

    public static final String DEFAULT_REDIS_TEMPLATE = "stringRedisTemplate";

    private String keyPrefix = RedisChatMemoryRepositoryConfig.DEFAULT_KEY_PREFIX;

    private String timeToLive = RedisChatMemoryRepositoryConfig.DEFAULT_TIME_TO_LIVE;

    public RedisChatMemoryRepositoryProperties() {}

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
    }
}
