/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer

@Configuration
@ConditionalOnProperty("app.storage-type", havingValue = "redis")
class RedisConfiguration(private val config: AppConfiguration) {

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory =
        LettuceConnectionFactory(RedisStandaloneConfiguration(config.redis.host, config.redis.port))

    @Bean
    fun byteRedisTemplate(redisConnectionFactory: RedisConnectionFactory) =
        RedisTemplate<String, ByteArray>().apply {
            connectionFactory = redisConnectionFactory
            keySerializer = RedisSerializer.string()
            valueSerializer = RedisSerializer.byteArray()
        }
}
