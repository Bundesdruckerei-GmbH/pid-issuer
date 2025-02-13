/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition

@Aspect
class TransactionalAop {
    @Autowired(required = false)
    private var transactionManager: DataSourceTransactionManager? = null

    @Pointcut("@annotation(transactionalOutsideBean)")
    fun callAt(transactionalOutsideBean: TransactionalOutsideBean) {
        // just the pointcut definition
    }

    @Around("callAt(transactionalOutsideBean)")
    fun withTransaction(
        joinPoint: ProceedingJoinPoint,
        transactionalOutsideBean: TransactionalOutsideBean,
    ): Any? {
        return if (transactionManager != null) {
            val td =
                DefaultTransactionDefinition().also {
                    it.isolationLevel = transactionalOutsideBean.isolation.value()
                    it.isReadOnly = transactionalOutsideBean.readOnly
                }

            val status = transactionManager!!.getTransaction(td)
            try {
                val result = joinPoint.proceed()
                transactionManager!!.commit(status)
                result
            } catch (e: Exception) {
                transactionManager!!.rollback(status)
                throw e
            }
        } else {
            joinPoint.proceed()
        }
    }
}
