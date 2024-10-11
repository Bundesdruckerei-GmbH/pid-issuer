/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
/**
 * This is the eid based identification module.
 * <p>
 * It uses the panstar sdk from governikus.
 */
@ApplicationModule(allowedDependencies = {"authorization::identificationApi", "base"})
package de.bdr.pidi.identification;

import org.springframework.modulith.ApplicationModule;