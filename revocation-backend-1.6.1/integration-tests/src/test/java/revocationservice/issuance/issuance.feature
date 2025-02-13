#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

Feature: verify the users endpoint

  Background:
    * call read('classpath:dsl.feature')
    * url baseURL

  @requirement=PIDI-2762,PIDI-2571
  Scenario: get count user with one pid
    * call fixedPseudonym
    * def getReferenceResponse = call takeReference
    * def statusListUri = getReferenceResponse.response.references[0].uri
    * def statusListIndex = getReferenceResponse.response.references[0].index
    * def pseudonymResponse = call takePseudonym
    * def pseudonym = pseudonymResponse.pseudonym
    * rmq.sendIssuance(pseudonym, statusListUri, statusListIndex)
    Given def sessionIdResponse = call login
    And header X-Session-ID = sessionIdResponse.sessionId
    And path 'users', 'issuances', 'count'
    When method get
    Then status 200
    And match response == {"issued": 1, "revocable": 1}
