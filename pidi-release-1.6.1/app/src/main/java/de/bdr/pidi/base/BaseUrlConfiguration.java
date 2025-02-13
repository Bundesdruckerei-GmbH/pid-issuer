/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.base;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Getter
public abstract class BaseUrlConfiguration {
    /**
     * the base url of the pid-issuer
     */
    private URL baseUrl;

    public void setBaseUrl(@NotNull URL baseUrl) throws MalformedURLException {
        if (baseUrl.getPath().endsWith("/")) {
            this.baseUrl = baseUrl;
        } else {
            this.baseUrl = URI.create(baseUrl + "/").toURL();
        }
    }
}
