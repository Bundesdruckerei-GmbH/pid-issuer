#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

Feature: mock stuff

  @ignore @called @getPseudonym
  Scenario: get mock pseudonym
    Given url eidMockBaseUrl
    And path 'pseudonym'
    When method get
    Then status 200
    * def pseudonym = response

  @ignore @called @pseudonymRandom
  Scenario: set mock random pseudonym for each authentication
    Given url eidMockBaseUrl
    And path 'pseudonym', 'random'
    When method post
    Then status 200

  @ignore @called @pseudonymFixedRandom
  Scenario: set mock random pseudonym but keep it over authentications
    Given url eidMockBaseUrl
    And path 'pseudonym', 'fixedRandom'
    When method post
    Then status 200