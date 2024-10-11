#!/bin/bash
#
# Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
#

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER $APP_USER WITH PASSWORD '$APP_USER_PASSWORD';
EOSQL