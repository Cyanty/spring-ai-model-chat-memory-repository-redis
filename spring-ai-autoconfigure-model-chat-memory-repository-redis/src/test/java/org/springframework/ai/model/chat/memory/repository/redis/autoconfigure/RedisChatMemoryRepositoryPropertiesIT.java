package org.springframework.ai.model.chat.memory.repository.redis.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepositoryConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisChatMemoryRepositoryPropertiesIT {

    @Test
    void defaultValues() {
        var props = new RedisChatMemoryRepositoryProperties();
        assertThat(props.getKeyPrefix())
                .isEqualTo(RedisChatMemoryRepositoryConfig.DEFAULT_KEY_PREFIX);
        assertThat(props.getTimeToLive())
                .isEqualTo(RedisChatMemoryRepositoryConfig.DEFAULT_TIME_TO_LIVE);
    }

    @Test
    void customValues() {
        var props = new RedisChatMemoryRepositoryProperties();
        props.setKeyPrefix("test_chat_memory:");
        props.setTimeToLive("1h");

        assertThat(props.getKeyPrefix()).isEqualTo("test_chat_memory:");
        assertThat(props.getTimeToLive()).isEqualTo("1h");
    }
}
