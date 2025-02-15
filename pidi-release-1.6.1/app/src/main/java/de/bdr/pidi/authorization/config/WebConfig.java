/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.config;

import de.bdr.pidi.authorization.config.interceptors.LoggingMdcInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoggingMdcInterceptor loggingMdcInterceptor;

    @Autowired
    public WebConfig(LoggingMdcInterceptor loggingMdcInterceptor) {
        this.loggingMdcInterceptor = loggingMdcInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingMdcInterceptor).addPathPatterns("/**");
    }
}
