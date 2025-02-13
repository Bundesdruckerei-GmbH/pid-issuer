#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: common code blocks for tests

  @ignore @getReference
  Scenario: get a reference
    * def poolIndex = karate.get('poolIndex', 0)
    * url internalApiURL
    Given path 'pools', pools[poolIndex].name, 'new-references'
    And header x-api-key = pools[poolIndex].apiKey
    When method post

  @ignore @getStatusList
  Scenario: get a status list
    * url baseURL
    * def format = karate.get('format', 'application/statuslist+json')
    Given path statusListId
    And header Accept = format
    When method get

  @ignore @updateStatus
  Scenario: update a status
    * def poolIndex = karate.get('poolIndex', 0)
    * def status = karate.get('status', 1)
    * url internalApiURL
    Given path 'status-lists', 'update'
    And header x-api-key = pools[poolIndex].apiKey
    And request {uri: #(statusUri), index: #(statusIndex), value: #(value)}
    When method patch

  @ignore @randomStatusListId
  Scenario: get a random status list id
    * def UUID = Java.type('java.util.UUID')
    * def randomId = UUID.randomUUID()