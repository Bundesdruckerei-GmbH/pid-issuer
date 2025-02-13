/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.pidi.identification.core;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Random;
@SecondaryPort
public interface RandomProvider {
    Random getTokenRng();
    Random getSessionRng();
    Random getSamlRng();
}
