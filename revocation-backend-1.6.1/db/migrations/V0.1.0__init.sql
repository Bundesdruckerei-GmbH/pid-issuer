--
-- Copyright 2024 Bundesdruckerei GmbH
-- For the license see the accompanying file LICENSE.MD.
--

CREATE TABLE IF NOT EXISTS issuances
(
    id                  BIGSERIAL                PRIMARY KEY,
    pseudonym           VARCHAR                  NOT NULL,
    list_id             VARCHAR                  NOT NULL,
    list_index          INTEGER                  NOT NULL,
    expiration_time     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked             BOOLEAN                  NOT NULL,
    CONSTRAINT uc_issuances_list_id_list_index UNIQUE (list_id, list_index)
);

CREATE INDEX IF NOT EXISTS issuances_pseudonym_idx ON issuances(pseudonym);

GRANT SELECT, INSERT, UPDATE, DELETE ON issuances TO ${APP_USER};
GRANT USAGE ON SEQUENCE issuances_id_seq TO ${APP_USER};
