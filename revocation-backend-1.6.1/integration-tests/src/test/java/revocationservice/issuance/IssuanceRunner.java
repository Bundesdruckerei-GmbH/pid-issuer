/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
package revocationservice.issuance;

import com.intuit.karate.junit5.Karate;

class IssuanceRunner {
    @Karate.Test
    Karate testIssuence() {
        return Karate.run("issuance").relativeTo(getClass());
    }
}
