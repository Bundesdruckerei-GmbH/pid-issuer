#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

# copied by   https://gitlab.partner.bdr.de/ssi4de/services/status-list-service/-/blob/main/integration-tests/src/test/java/statuslistservice/common/common.feature


Feature: common code blocks for tests

  @ignore @getReference
  Scenario: get a reference
  * def poolIndex = karate.get('poolIndex', 0)
  * url statusListServiceURL
  Given path 'pools', pools[poolIndex].name, 'new-references'
  And header x-api-key = pools[poolIndex].apiKey
  When method post