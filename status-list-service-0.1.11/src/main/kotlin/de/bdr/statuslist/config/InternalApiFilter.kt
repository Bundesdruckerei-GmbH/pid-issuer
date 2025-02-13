/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.md
 */
package de.bdr.statuslist.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

class InternalApiFilter(val internalPort: Int, val internalApiPrefixes: List<String>) : Filter {
    private val BAD_REQUEST =
        """
       {"code":${HttpStatus.BAD_REQUEST.value()},"error":true,"errorMessage":"${HttpStatus.BAD_REQUEST.reasonPhrase}"} 
    """
            .toByteArray(Charsets.UTF_8)
    private val log = LoggerFactory.getLogger(InternalApiFilter::class.java)

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        val isInternalApi =
            internalApiPrefixes.stream().anyMatch {
                (servletRequest as HttpServletRequest).requestURI.startsWith(it)
            }
        val isInternalPort = servletRequest.localPort == internalPort
        if ((isInternalApi && !isInternalPort) || (!isInternalApi && isInternalPort)) {
            log.warn(
                "Deny the request {} on port {}",
                (servletRequest as HttpServletRequest).requestURI,
                servletRequest.localPort,
            )
            (servletResponse as HttpServletResponse).status = HttpStatus.BAD_REQUEST.value()

            servletResponse.outputStream.write(BAD_REQUEST)
            servletResponse.outputStream.close()
        } else {
            filterChain.doFilter(servletRequest, servletResponse)
        }
    }
}
