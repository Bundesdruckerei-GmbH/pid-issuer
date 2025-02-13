/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.authorization.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.bdr.pidi.authorization.out.identification.PidCredentialData;
import de.bdr.pidi.base.PidServerException;
import org.springframework.stereotype.Component;

@Component
public class PidSerializer {

    private final ObjectMapper mapper;

    public PidSerializer() {
        this.mapper = JsonMapper.builder()
                .findAndAddModules().build();
    }

    public String toString(PidCredentialData pid) {
        try {
            return this.mapper.writeValueAsString(pid);
        } catch (JsonProcessingException e) {
            throw new PidServerException("could not convert pid to string", e);
        }
    }

    public PidCredentialData fromString(String s) {
        try {
            return this.mapper.readValue(s, PidCredentialData.class);
        } catch (JsonProcessingException e) {
            throw new PidServerException("could not read pid from string", e);
        }
    }
}