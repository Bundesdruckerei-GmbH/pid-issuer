#!/bin/bash
# Copyright (c) 2021,2022,2024 by Bundesdruckerei GmbH

if [[ -z "$LIQUIBASE_COMMAND_USERNAME" ]]
then
  echo "LIQUIBASE_COMMAND_USERNAME environment variable is missing"
  exit 1
fi

if [[ -z "$LIQUIBASE_COMMAND_PASSWORD" ]]
then
  echo "LIQUIBASE_COMMAND_PASSWORD environment variable is missing"
  exit 1
fi

if [[ -z "$LIQUIBASE_COMMAND_CONTEXT_FILTER" ]]
then
  echo "LIQUIBASE_COMMAND_CONTEXT_FILTER environment variable is missing"
  exit 1
fi

if [[ -z "$LIQUIBASE_COMMAND_URL" ]]
then
  echo "LIQUIBASE_COMMAND_URL environment variable is missing"
  exit 1
fi

# if running local wait for db accepting connections
if [[ "$RUN_LOCAL" == "true" ]]
then
  timeout 22 bash -c 'until printf "" 2>>/dev/null >>/dev/tcp/$0/$1; do sleep 1; done' registrydb 5432
fi

JAVA_OPTS="-Djavax.net.ssl.trustStore=${CLIENT_SSL_OUTBOUND_TRUST_STORE} -Djavax.net.ssl.trustStorePassword=${CLIENT_SSL_OUTBOUND_TRUST_STORE_PASSWORD} -showversion"

liquibase --changeLogFile=db/changelog/db.changelog-master.yaml --logLevel=info update
