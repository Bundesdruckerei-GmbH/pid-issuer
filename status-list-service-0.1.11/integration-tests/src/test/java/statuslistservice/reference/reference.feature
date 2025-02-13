#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: verify the reference endpoint

  Background:
    * url internalApiURL

  Scenario: take a reference from the first pool
    Given path 'pools', pools[0].name, 'new-references'
    And header x-api-key = pools[0].apiKey
    When method post
    Then status 200
    And match response.references[0] == {uri: '#present', index: #number}

  Scenario: get error for reference from the first pool with wrong api key
    Given path 'pools', pools[0].name, 'new-references'
    And header x-api-key = 'abc'
    When method post
    Then status 401
    And match response == {"code":"UNAUTHORIZED","message":"Invalid api key"}

  Scenario: get error for reference from the unknown pool
    Given path 'pools', 'abc', 'new-references'
    And header x-api-key = pools[0].apiKey
    When method post
    Then status 404
    And match response == {"code":"NO_SUCH_POOL","message":"No pool with id abc"}

  Scenario: take 100 references from the first pool
    Given path 'pools', pools[0].name, 'new-references'
    And params {amount: '100'}
    And header x-api-key = pools[0].apiKey
    When method post
    Then status 200
    And match response.references == '#[100]'
    And match response.references[0] == {uri: '#present', index: #number}

  @env=local_api_port
  Scenario: use external port
    * url baseURL
    Given path 'pools', pools[0].name, 'new-references'
    And header x-api-key = pools[0].apiKey
    When method post
    Then status 400
    And match response == {"code":400,"error":true,"errorMessage":"Bad Request"}
