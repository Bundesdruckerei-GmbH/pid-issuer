/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.adapter.in.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ServletSupport {

    public HttpServletRequest getServletRequest() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        Objects.requireNonNull(requestAttributes, "no request attributes present");
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }
}
