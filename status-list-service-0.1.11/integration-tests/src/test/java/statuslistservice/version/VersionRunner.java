/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.version;

import com.intuit.karate.junit5.Karate;

class VersionRunner {
    @Karate.Test
    Karate testVersion() {
        return Karate.run("version").relativeTo(getClass());
    }
}
