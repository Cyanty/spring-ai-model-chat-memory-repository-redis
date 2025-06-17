package org.springframework.ai.model.chat.memory.repository.redis.autoconfigure;

import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Auto-configuration for {@link RedisChatMemoryRepository }
 *
 * @author Cyanty
 * @since 1.0.0
 */
@AutoConfiguration(after = RedisAutoConfiguration.class, before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({RedisChatMemoryRepository.class, RedisTemplate.class})
@EnableConfigurationProperties({RedisChatMemoryRepositoryProperties.class})
public class RedisChatMemoryRepositoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisChatMemoryRepository redisChatMemoryRepository(
            @Qualifier(RedisChatMemoryRepositoryProperties.DEFAULT_REDIS_TEMPLATE) RedisTemplate<String, String> redisTemplate,
            RedisChatMemoryRepositoryProperties properties) {

        return RedisChatMemoryRepository.builder()
                .keyPrefix(properties.getKeyPrefix())
                .timeToLive(properties.getTimeToLive())
                .redisTemplate(redisTemplate)
                .build();
    }
}
