#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: verify the version endpoint

  Background:
    * url baseURL

  Scenario: get the current version
    Given path 'version'
    When method get
    Then status 200

  @env=local_api_port
  Scenario: use internal api port
    * url internalApiURL
    Given path 'version'
    When method get
    Then status 400
    And match response == {"code":400,"error":true,"errorMessage":"Bad Request"}
