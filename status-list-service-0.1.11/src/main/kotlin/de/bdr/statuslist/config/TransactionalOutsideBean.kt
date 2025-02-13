/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import org.springframework.transaction.annotation.Isolation

/** For transactional use outside of Spring managed beans */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TransactionalOutsideBean(
    val isolation: Isolation = Isolation.DEFAULT,
    val readOnly: Boolean = false,
)
