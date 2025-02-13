#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

@ignore
  Feature:
    Scenario:
      * def takeReference = read('classpath:statuslistservice/common.feature@getReference')

      * def authUrl = read('classpath:revocationservice/identification/eidAuth.feature@auth-url')
      * def auth = read('classpath:revocationservice/identification/eidAuth.feature@auth')
      * def loggedIn = read('classpath:revocationservice/identification/eidAuth.feature@loggedIn')
      * def login = read('classpath:revocationservice/identification/eidAuth.feature@login')
      * def dropAuth = read('classpath:revocationservice/identification/eidAuth.feature@dropAuth')

      * def takePseudonym = read('classpath:eidmock/mock.feature@getPseudonym')
      * def randomPseudonym = read('classpath:eidmock/mock.feature@pseudonymRandom')
      * def fixedPseudonym = read('classpath:eidmock/mock.feature@pseudonymFixedRandom')