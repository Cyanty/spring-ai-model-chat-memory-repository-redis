package org.springframework.ai.chat.memory.repository.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class RedisChatMemoryRepositoryConfigIT {

    @Test
    public void testBuilderWithDefaultExplicit() {
        var repository =
                RedisChatMemoryRepositoryConfig.builder()
                        .withRedisTemplate(mock(RedisTemplate.class))
                        .build();

        assertThat(repository).isNotNull();
        assertThat(repository.getKeyPrefix())
                .isEqualTo(RedisChatMemoryRepositoryConfig.DEFAULT_KEY_PREFIX);
        assertThat(repository.getTimeToLive()).isEqualTo(-1L);
    }

    @ParameterizedTest
    @CsvSource({"1s,1", "1m,60", "1h,3600", "1d,86400", "-1,-1", "1xx,-1"})
    public void testBuilderWithExplicitTimeToLive(
            String timeToLive, long expectedTimeToLiveMillis) {
        var repository =
                RedisChatMemoryRepositoryConfig.builder()
                        .withKeyPrefix("test_chat_memory:")
                        .withTimeToLive(timeToLive)
                        .withRedisTemplate(mock(RedisTemplate.class))
                        .build();

        assertThat(repository).isNotNull();
        assertThat(repository.getKeyPrefix()).isEqualTo("test_chat_memory:");
        assertThat(repository.getTimeToLive()).isEqualTo(expectedTimeToLiveMillis);
    }
}
