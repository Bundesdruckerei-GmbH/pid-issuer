#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

Feature: verify the version endpoint

#  Background:
#    * url baseURL

  @ignore @called @auth-url
  Scenario: get auth-url
    Given url baseURL
    And path 'eid', 'auth-url'
    When method get
    Then status 200
    * match response == {tcTokenUrl: '#present', sessionId: '#present', duration: #number}
    * def tcTokenUrl = karate.urlDecode(response.tcTokenUrl)
    * def sessionId = response.sessionId
    * def sessionDuration = response.duration

  @ignore @called @auth
  Scenario: auth
    * configure followRedirects = false
    Given url eidMockBaseUrl
    And path 'eID-Client'
    And param tcTokenURL = tcTokenUrl
    And header karate-name = 'GET eid client'
    When method get
    Then status 303
    * def location = responseHeaders['location'][0]

  @ignore @called @loggedIn
  Scenario: loggedIn
    * def ref = location.substring(location.indexOf("=") + 1)
    * configure followRedirects = false
    Given url baseURL
    And path 'eid', 'auth-loggedin'
    And param ref = ref
    And header X-Session-ID = sessionId
    And header karate-name = 'GET eid/auth-loggedin'
    When method get
    Then status 200
    And match response !contains {sessionId: #(sessionId)}
    * def sessionId = response.sessionId

  @ignore @called @dropAuth
  Scenario: dropAuth
    Given url baseURL
    And path 'eid', 'auth-drop'
    And header X-Session-ID = sessionId
    When method get
    Then status 200

  @ignore @called @login
  Scenario: login
    * def test = call read('@auth-url')
    * def authResult = call read('@auth') {tcTokenUrl : #(test.tcTokenUrl)}
    * def renewResult = call read('@loggedIn') {location: #(authResult.location), sessionId: #(test.sessionId)}
    * def sessionId = renewResult.sessionId