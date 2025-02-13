/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package eidmock;

import com.intuit.karate.junit5.Karate;

class mockRunner {
    @Karate.Test
    Karate testMock() {
        return Karate.run("mock").relativeTo(getClass());
    }
}
