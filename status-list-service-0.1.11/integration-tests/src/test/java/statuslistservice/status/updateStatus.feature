#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature: update status on a list

  Background:
    * url internalApiURL
    * call read('classpath:dsl.feature')
    * def pollStatusList =
    """
    function(options) {
       var retries = 20
       const sleep = 1000
       var lastResult = karate.call('classpath:/statuslistservice/common/getStatus.feature@getStatus', options).bitString;
       karate.log('first call: ', lastResult)
       while(true) {
         var response = karate.call('classpath:/statuslistservice/common/getStatus.feature@getStatus', options);
         if(lastResult != response.bitString) {
           return response.bitString
         }
         if(retries == 0) {
           return response.bitString
         }
         lastResult = response.bitString
         karate.log('poll response', lastResult);
         retries = retries - 1
         java.lang.Thread.sleep(sleep)
      }
    }
    """

  Scenario: take a reference from the first pool and set a status
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: #(statusUri), index: #(statusIndex), value: 1}
    When method patch
    Then status 204

  Scenario: get error for update with invalid api key
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = 'abc'
    And request {uri: #(statusUri), index: #(statusIndex), value: 1}
    When method patch
    Then status 401
    And match response == {"code":"UNAUTHORIZED","message":"Invalid api key"}

  Scenario: get error for update with different api key
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[1].apiKey
    And request {uri: #(statusUri), index: #(statusIndex), value: 1}
    When method patch
    Then status 401
    And match response == {"code":"UNAUTHORIZED","message":"Invalid api key"}

  Scenario: get error for update with wrong uri in body
    Given def takeReferenceResponse = call takeReference
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: 'abc', index: #(statusIndex), value: 1}
    When method patch
    Then status 400
    And match response == {"code":'#string',"message":'#string'}
    And match response == {"code":"NO_SUCH_LIST","message":"listUri abc invalid"}

  Scenario: get error for update with wrong index in body
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: #(statusUri), index: 'abc', value: 1}
    When method patch
    Then status 400
    And match response == {"code":'#string',"message":'#string'}
    And match response == {"code":"BAD_REQUEST","message":"JSON parse error: Cannot deserialize value of type `int` from String \"abc\": not a valid `int` value"}

  Scenario: get error for update with index out of bound
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: #(statusUri), index: 999, value: 1}
    When method patch
    Then status 400
    And match response == {"code":'#string',"message":'#string'}
    And match response.code == "INDEX_OUT_OF_BOUNDS"
    And match response.message contains "Index out of bounds"

  Scenario: get error for update with wrong value in body
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: #(statusUri), index: #(statusIndex), value: 'aa'}
    When method patch
    Then status 400
    And match response == {"code":'#string',"message":'#string'}
    And match response == {"code":"BAD_REQUEST","message":"JSON parse error: Cannot deserialize value of type `int` from String \"aa\": not a valid `int` value"}

  Scenario: get error for update with missing value in body
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    And request {uri: #(statusUri), index: #(statusIndex)}
    When method patch
    Then status 400
    And match response == {"code":'#string',"message":'#string'}
    And match response == {"code":"BAD_REQUEST","message":"JSON parse error: Missing required creator property 'value' (index 2)"}

  Scenario: update a status which is bigger then the list entries
    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And path 'status-lists', 'update'
    And header x-api-key = pools[0].apiKey
    # the size of entries is one bit, but 5 needs 3 bits
    And request {uri: #(statusUri), index: #(statusIndex), value: 5}
    When method patch
    Then status 400
    And match response == {code: "VALUE_OUT_OF_RANGE", message: '#present'}

  Scenario: update status and poll status-list until new status is applied

    Given def takeReferenceResponse = call takeReference
    And def statusUri = takeReferenceResponse.response.references[0].uri
    And def statusListId = statusUri.substring(statusUri.lastIndexOf('/') + 1)
    And def statusIndex = takeReferenceResponse.response.references[0].index
    And def updateStatusResponse = call updateStatus {statusUri: #(statusUri), statusIndex: #(statusIndex), value: 1}
    When def bitString = call pollStatusList {statusListId: #(statusListId), statusIndex: #(statusIndex)}
    Then match bitString == '1'

