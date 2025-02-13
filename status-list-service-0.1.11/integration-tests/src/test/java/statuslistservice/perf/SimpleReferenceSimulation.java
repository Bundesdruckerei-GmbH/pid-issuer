/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.perf;

import com.intuit.karate.gatling.javaapi.KarateProtocolBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static com.intuit.karate.gatling.javaapi.KarateDsl.karateFeature;
import static com.intuit.karate.gatling.javaapi.KarateDsl.karateProtocol;
import static com.intuit.karate.gatling.javaapi.KarateDsl.uri;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;

public class SimpleReferenceSimulation extends Simulation {
    public SimpleReferenceSimulation() {
        KarateProtocolBuilder protocol = karateProtocol(
                // adding uri patterns, so gatling can group them correct
                uri("pools/{pool}/new-references").nil(),
                uri("pools/{pool}/new-references?amount={amount}").nil(),
                uri("{listId}").nil());
        ScenarioBuilder newReference = scenario("newReference").exec(karateFeature("classpath:statuslistservice/reference/reference.feature"));
        ScenarioBuilder getStatusList = scenario("getStatusList").exec(karateFeature("classpath:statuslistservice/status/statusList.feature"));
        ScenarioBuilder updateStatusList = scenario("updateStatusList").exec(karateFeature("classpath:statuslistservice/status/updateStatus.feature"));
        setUp(
                newReference.injectOpen(constantUsersPerSec(5).during(10)),
                getStatusList.injectOpen(constantUsersPerSec(5).during(10)),
                updateStatusList.injectOpen(constantUsersPerSec(5).during(10))
        ).protocols(protocol);

    }
}
