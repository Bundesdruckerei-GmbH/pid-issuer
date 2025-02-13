/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EidTypeListenerDummy implements EidTypeListener {

    @Override
    public void eidTypeUsed(String eidType) {
        log.info("eidTypeUsed: {}", eidType);
    }
}
