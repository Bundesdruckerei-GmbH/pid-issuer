/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.authorization.core.particle;

import de.bdr.pidi.authorization.FlowVariant;
import de.bdr.pidi.authorization.core.WSessionImpl;
import de.bdr.pidi.authorization.core.domain.Requests;
import de.bdr.pidi.authorization.core.exception.InvalidRequestException;
import de.bdr.pidi.testdata.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestOrderValidationHandlerTest {
    private final RequestOrderValidationHandler requestOrderValidationHandlerUT = new RequestOrderValidationHandler();

    @Test
    void test001() {
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.setNextExpectedRequest(Requests.PUSHED_AUTHORIZATION_REQUEST);

        assertDoesNotThrow(() -> requestOrderValidationHandlerUT.processPushedAuthRequest(null, null, session));
    }

    @Test
    void test002() {
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);

        assertThrows(InvalidRequestException.class, () -> requestOrderValidationHandlerUT.processPushedAuthRequest(null, null, session));
    }

    @Test
    void test003() {
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.setNextExpectedRequest(Requests.AUTHORIZATION_REQUEST);

        assertDoesNotThrow(() -> requestOrderValidationHandlerUT.processAuthRequest(null, null, session, true));
    }

    @Test
    void test004() {
        WSessionImpl session = new WSessionImpl(FlowVariant.C, TestUtils.randomSessionId());
        session.setNextExpectedRequest(Requests.PUSHED_AUTHORIZATION_REQUEST);

        assertThrows(InvalidRequestException.class, () -> requestOrderValidationHandlerUT.processAuthRequest(null, null, session, true));
    }
}
