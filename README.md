# Redis Chat Memory Repository for Spring AI (v1.0.0)

## Overview

This project provides a Redis-based implementation of Spring AI's ChatMemoryRepository interface, enabling storage of conversation histories for AI chat applications. 

## Key Features

- Conversation Histories Storage: Store conversation histories in Redis for horizontal scalability
- Spring Boot Auto-Configuration: Zero-config setup with Spring Boot's autoconfiguration
- TTL Support: Automatic expiration of inactive conversations
- Custom Serialization: Efficient JSON serialization of chat messages

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>spring-ai-starter-model-chat-memory-repository-redis</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```

### 2. Configure Redis Connection

Add to `application.yml`:

```yaml
spring:
  ai:
    chat:
      memory:
        repository:
          redis:
            key-prefix: "my_chat_memory:"
            time-to-live: "7d"
  data:
    redis:
      host: "localhost"
      port: 6379
      database: 0
      # username:
      # password:
```

### 3. Use in Your Application

```java
@Autowired
RedisChatMemoryRepository redisChatMemoryRepository

// or Manual configuration
RedisChatMemoryRepository.builder().redisTemplate(redisTemplate).build();
```

## Configuration Options

|                       Property                        |         Default          |         Description          |
| :---------------------------------------------------: | :----------------------: | :--------------------------: |
|  `spring.ai.chat.memory.repository.redis.key-prefix`  | `spring_ai_chat_memory:` |       Redis key prefix       |
| `spring.ai.chat.memory.repository.redis.time-to-live` |           `-1`           | Conversation expiration time |

## License

This project is released under the Apache License 2.0.
