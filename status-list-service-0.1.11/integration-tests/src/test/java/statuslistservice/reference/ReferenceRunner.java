/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.reference;

import com.intuit.karate.junit5.Karate;

class ReferenceRunner {
    @Karate.Test
    Karate testReference() {
        return Karate.run("reference").relativeTo(getClass());
    }
}
