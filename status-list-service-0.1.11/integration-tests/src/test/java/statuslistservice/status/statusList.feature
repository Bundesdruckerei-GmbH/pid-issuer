#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: retrieve a status list

  Background:
    * url baseURL
    * call read('classpath:dsl.feature')
    * def listId = call randomStatusListId
    * def listId = listId.randomId

  Scenario: get a random status list fail with error message
    Given path listId
    When method get
    Then status 404
    And match response.code == 'NO_SUCH_LIST'
    And match response.message == 'No list with id '+listId

  Scenario: get a valid status list as json
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def referenceId = statusUri.substring(statusUri.lastIndexOf('/') + 1)
    And def aggregationUri = baseURL + "/aggregation/" + pools[0].name
    And path referenceId
    When header accept = "application/statuslist+json"
    And method get
    Then status 200
    And match response == {"bits":1,"lst":'#string',"aggregation_uri":'#(aggregationUri)'}


  Scenario: check valid list is present in available lists
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And path "/aggregation/",pools[0].name
    When method get
    Then status 200
    And match response == {'status_lists':#array}
    And match response.status_lists contains [#(statusUri)]

  Scenario: get error for status list request with invalid pool_id
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And path "/aggregation/","invalid_pool_id"
    When method get
    Then status 404
    And match response == {"code":'#string',"message":'#string'}
    And match response == {"code":"NO_SUCH_POOL","message":"Missing entry"}

  @env=local_api_port
  Scenario: use internal api port
    * url internalApiURL
    Given path listId
    When method get
    Then status 400
    And match response == {"code":400,"error":true,"errorMessage":"Bad Request"}
