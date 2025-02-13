/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.status;

import com.intuit.karate.junit5.Karate;

class StatusRunner {
    @Karate.Test
    Karate testUpdateStatus() {
        return Karate.run("updateStatus").relativeTo(getClass());
    }

    @Karate.Test
    Karate testListStatus() {
        return Karate.run("statusList").relativeTo(getClass());
    }
}
