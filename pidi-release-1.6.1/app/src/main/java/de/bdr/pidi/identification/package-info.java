/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
/**
 * This is the eid based identification module.
 * <p>
 * It uses the panstar sdk from governikus.
 */
@ApplicationModule(allowedDependencies = {"authorization::identificationApi", "base"})
package de.bdr.pidi.identification;

import org.springframework.modulith.ApplicationModule;