/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication @ConfigurationPropertiesScan @EnableAsync class StatusListServiceApplication

fun main(args: Array<String>) {
    runApplication<StatusListServiceApplication>(*args)
}
