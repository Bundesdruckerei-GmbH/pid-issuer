/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
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
