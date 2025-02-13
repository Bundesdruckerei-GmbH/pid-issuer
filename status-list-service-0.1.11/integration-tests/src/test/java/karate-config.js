/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
function fn() {
    let env = karate.env; // get system property 'karate.env'
    karate.log('karate.env system property was:', env);
    if (!env) {
        env = 'local';
    }
    karate.log('using env: ', env);
    const config = {
        env: env,
        baseURL: 'http://localhost:8085',
        internalApiURL: 'http://localhost:8085',
        pools: []
    };
    karate.configure('ssl', true); // ignore self signed ssl certs
    if (env === 'local') {
        config.pools[0] = {name: "verified-email", apiKey: "366A9069-2965-4667-9AD2-5C51D71046D8"}
        config.pools[1] = {name: "test-pool-2", apiKey: "366A9069-2965-4667-9AD2-5C51D71046D3"}
    }
    return config;
}

