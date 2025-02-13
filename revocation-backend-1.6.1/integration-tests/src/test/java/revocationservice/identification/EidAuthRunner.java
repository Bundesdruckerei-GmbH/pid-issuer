/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package revocationservice.identification;

import com.intuit.karate.junit5.Karate;

class EidAuthRunner {
    @Karate.Test
    Karate testEidAuth() {
        return Karate.run("eidAuth").relativeTo(getClass());
    }
}
