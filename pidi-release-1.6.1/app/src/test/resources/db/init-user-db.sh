#!/bin/bash
#
# Copyright 2024 Bundesdruckerei GmbH
# For the license see the accompanying file LICENSE.MD.
#

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER $APP_USER WITH PASSWORD '$APP_USER_PASSWORD';
EOSQL