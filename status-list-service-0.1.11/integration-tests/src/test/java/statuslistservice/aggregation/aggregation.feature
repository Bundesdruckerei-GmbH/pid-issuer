#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: get an aggregation

  Background:
    * url baseURL
    * call read('classpath:dsl.feature')

  Scenario: get a reference, get a status list and get an aggregation
    * def getReferenceResponse = call takeReference
    * def statusList = getReferenceResponse.response.references[0].uri
    * def statusListId = statusList.substring(statusList.lastIndexOf('/') + 1)
    Given def statusListResponse = call getStatusList {'statusListId' : #(statusListId)}
    * url statusListResponse.response.aggregation_uri
    When method get
    Then status 200

