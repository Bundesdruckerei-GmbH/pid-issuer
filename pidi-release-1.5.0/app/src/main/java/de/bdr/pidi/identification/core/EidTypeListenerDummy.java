/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
