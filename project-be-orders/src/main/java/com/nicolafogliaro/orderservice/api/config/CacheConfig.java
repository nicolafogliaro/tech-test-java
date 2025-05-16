package com.nicolafogliaro.orderservice.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicolafogliaro.orderservice.api.dto.order.OrderResponse;
import com.nicolafogliaro.orderservice.api.dto.product.ProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.time.Duration;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;
import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    public static final String PRODUCTS_CACHE_NAME = "products";
    public static final String PRODUCT_CACHE_NAME = "product";
    public static final String ORDER_CACHE_NAME = "order";


    @Value("${spring.redis.host:localhost}")
    private String redisUrl;

    @Value("${spring.redis.port:6379}")
    private Integer redisPort;

    @Value("${redis.ttl:3600}")
    private long cacheTtl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info(">>> Redis Config: url: {}, port: {}", redisUrl, redisPort);
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisUrl, redisPort));
        log.info("<<< Redis Config: url: {}, port: {}", redisUrl, redisPort);
        return lettuceConnectionFactory;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisBuilderCustomizer(ObjectMapper objectMapper) {

        ObjectMapper cacheObjectMapper = objectMapper.copy();

        cacheObjectMapper.activateDefaultTyping(cacheObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                PROPERTY);

        GenericJackson2JsonRedisSerializer defaultSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        //I would rather create Jackson2JsonRedisSerializer for each cached model to ensure our own model's freedom
        Jackson2JsonRedisSerializer<OrderResponse> employeeJackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, OrderResponse.class);
        Jackson2JsonRedisSerializer<ProductResponse> departmentJackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, ProductResponse.class);

        return builder -> {
            builder.cacheDefaults(defaultCacheConfig()
                            .serializeValuesWith(fromSerializer(defaultSerializer))
                            .entryTtl(Duration.ofSeconds(cacheTtl))
                    )
                    .withCacheConfiguration(ORDER_CACHE_NAME, defaultCacheConfig()
                            .serializeValuesWith(fromSerializer(employeeJackson2JsonRedisSerializer))
                            .entryTtl(Duration.ofSeconds(cacheTtl))
                    )
                    .withCacheConfiguration(PRODUCT_CACHE_NAME, defaultCacheConfig()
                            .serializeValuesWith(fromSerializer(departmentJackson2JsonRedisSerializer))
                            .entryTtl(Duration.ofSeconds(cacheTtl))
                    )
                    .withCacheConfiguration(PRODUCTS_CACHE_NAME, defaultCacheConfig()
                            .serializeValuesWith(fromSerializer(departmentJackson2JsonRedisSerializer))
                            .entryTtl(Duration.ofSeconds(cacheTtl))
                    );
        };
    }
}
