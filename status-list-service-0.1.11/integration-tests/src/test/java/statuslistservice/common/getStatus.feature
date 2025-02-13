#
# Copyright 2024 Bundesdruckerei GmbH
#

Feature:
  @ignore @getStatus
  Scenario: get a status bit string
    * print 'index: ', statusIndex
    * def StatusListUtils = Java.type('statuslistservice.status.StatusListUtils')
    * def getStatusListResponse = call read('classpath:/statuslistservice/common/common.feature@getStatusList') {statusListId: #(statusListId)}
    * def bitString = StatusListUtils.getByteString(getStatusListResponse.response.bits, getStatusListResponse.response.lst, statusIndex)