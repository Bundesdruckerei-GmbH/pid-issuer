/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package de.bdr.revocation.identification.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdentificationExceptionTest {

    @Test
    void when_ctor_with_message_then_OK() {
        String message = "foo";

        var testee = new IdentificationException(message);

        Assertions.assertEquals(message, testee.getMessage());
        Assertions.assertEquals(IdentificationException.BASE_ERROR_CODE, testee.getCode());
    }

    @Test
    void when_ctor_with_messageAnd_code_then_OK() {
        String message = "foo";
        int code = IdentificationException.BASE_ERROR_CODE+42;

        var testee = new IdentificationException(message, code);

        Assertions.assertEquals(message, testee.getMessage());
        Assertions.assertEquals(code, testee.getCode());
    }

    @Test
    void when_ctor_withMessageAnd_badCode_then_OK() {
        String message = "foo";
        int code = 123;

        var testee = new IdentificationException(message, code);

        Assertions.assertEquals(message, testee.getMessage());
        Assertions.assertEquals(IdentificationException.BASE_ERROR_CODE, testee.getCode());
    }

    @Test
    void when_ctor_with_messageAndException_then_OK() {
        String message = "fbar";
        Exception ex = new IllegalArgumentException();

        var testee = new IdentificationException(message, ex);

        Assertions.assertEquals(message, testee.getMessage());
        Assertions.assertEquals(ex, testee.getCause());
        Assertions.assertEquals(IdentificationException.BASE_ERROR_CODE, testee.getCode());
    }
}