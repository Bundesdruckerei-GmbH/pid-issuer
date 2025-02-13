/*
 * Copyright 2024 Bundesdruckerei GmbH
 * For the license see the accompanying file LICENSE.MD.
 */
function fn() {
    let env = karate.env; // get system property 'karate.env'
    karate.log('karate.env system property was:', env);
    if (!env) {
        env = 'local';
    }
    var Rmq = Java.type('revocationservice.issuance.rabbitmq.RmqUtils');
    karate.log('using env: ', env);
    const config = {
        env: env,
        rabbitMqPort: 5672,
        pools: []
    };
    karate.configure('ssl', true); // ignore self signed ssl certs

    if (env === 'local') {
        config.baseURL = 'http://localhost:8080'
        config.statusListServiceURL = 'http://localhost:8085'
        config.eidMockBaseUrl = 'http://localhost:24727'
        config.rabbitMqUsername = 'guest'
        config.rabbitMqPassword = 'guest'
        config.rabbitMqHost = 'localhost'
        config.pools[0] = {name: "verified-email", apiKey: "366A9069-2965-4667-9AD2-5C51D71046D8"}
    }

    config.rmq = new Rmq('revocation-service.issuance-provided', config.rabbitMqUsername, config.rabbitMqPassword, config.rabbitMqHost, config.rabbitMqPort)

    return config;
}

