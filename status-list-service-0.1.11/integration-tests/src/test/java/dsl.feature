#
# Copyright 2024 Bundesdruckerei GmbH
#

@ignore
  Feature:
    Scenario:
      * def takeReference = read('classpath:/statuslistservice/common/common.feature@getReference')
      * def getStatusList = read('classpath:/statuslistservice/common/common.feature@getStatusList')
      * def getStatus = read('classpath:/statuslistservice/common/getStatus.feature@getStatus')
      * def updateStatus = read('classpath:/statuslistservice/common/common.feature@updateStatus')
      * def randomStatusListId = read('classpath:/statuslistservice/common/common.feature@randomStatusListId')