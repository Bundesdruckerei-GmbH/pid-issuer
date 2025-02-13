/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import org.aspectj.lang.Aspects
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("app.storage-type", havingValue = "postgres")
class PostgreSqlConfiguration {
    @Bean
    fun transactionalAop(): TransactionalAop {
        return Aspects.aspectOf(TransactionalAop::class.java)
    }
}
