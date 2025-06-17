package org.springframework.ai.chat.memory.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/** redis chat memory repository config */
public class RedisChatMemoryRepositoryConfig {

    public static final String DEFAULT_KEY_PREFIX = "spring_ai_chat_memory:";

    public static final String DEFAULT_TIME_TO_LIVE = "-1";

    private final String keyPrefix;

    private final long timeToLive;

    private final RedisTemplate<String, String> redisTemplate;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public RedisTemplate<String, String> getRedisTemplate() {
        return redisTemplate;
    }

    private RedisChatMemoryRepositoryConfig(Builder builder) {
        this.keyPrefix = builder.keyPrefix;
        this.timeToLive = parseTimeToSeconds(builder.timeToLive);
        this.redisTemplate = builder.redisTemplate;
    }

    private long parseTimeToSeconds(String time) {
        if (time == null || time.isEmpty()) {
            return -1;
        }

        String iso8601Duration = convertToIso8601(time);
        if (iso8601Duration.equals("-1")) {
            return -1;
        }

        Duration duration = Duration.parse(iso8601Duration);
        return duration.getSeconds();
    }

    private String convertToIso8601(String time) {
        String value = time.replaceAll("[^\\d]", "");
        String unit = time.replaceAll("[\\d]", "");

        return switch (unit) {
            case "s" -> "PT" + value + "S";
            case "m" -> "PT" + value + "M";
            case "h" -> "PT" + value + "H";
            case "d" -> "P" + value + "D";
            default -> "-1"; // Invalid match defaults to returning -1
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    /** RedisChatMemoryRepositoryConfig Builder */
    public static final class Builder {

        private String keyPrefix = DEFAULT_KEY_PREFIX;

        private String timeToLive = DEFAULT_TIME_TO_LIVE;

        private RedisTemplate<String, String> redisTemplate;

        private Builder() {}

        public String getKeyPrefix() {
            return this.keyPrefix;
        }

        public String getTimeToLive() {
            return this.timeToLive;
        }

        public RedisTemplate<String, String> getRedisTemplate() {
            return this.redisTemplate;
        }

        public Builder withKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public Builder withTimeToLive(String timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder withRedisTemplate(RedisTemplate<String, String> redisTemplate) {
            this.redisTemplate = redisTemplate;
            return this;
        }

        public RedisChatMemoryRepositoryConfig build() {
            return new RedisChatMemoryRepositoryConfig(this);
        }
    }
}
