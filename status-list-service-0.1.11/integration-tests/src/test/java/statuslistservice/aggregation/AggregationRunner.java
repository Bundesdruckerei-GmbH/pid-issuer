/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.aggregation;

import com.intuit.karate.junit5.Karate;

class AggregationRunner {
    @Karate.Test
    Karate testAggregation() {
        return Karate.run("aggregation").relativeTo(getClass());
    }

}
