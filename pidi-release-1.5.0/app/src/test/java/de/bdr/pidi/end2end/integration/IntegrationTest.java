/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.end2end.integration;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base class of the integration tests. Use this as a base class whenever you need the spring application to start.
 * All tests extending this class share the spring application - so it's only started once.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class IntegrationTest {
}
