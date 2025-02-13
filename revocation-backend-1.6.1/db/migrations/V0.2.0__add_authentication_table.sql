--
-- Copyright 2024 Bundesdruckerei GmbH
-- For the license see the accompanying file LICENSE.MD.
--

CREATE TABLE IF NOT EXISTS authentication
(
    id                      BIGSERIAL                   PRIMARY KEY,
    authentication_state    VARCHAR                     NOT NULL,
    session_id              VARCHAR                     NOT NULL UNIQUE,
    token_id                VARCHAR                     NOT NULL UNIQUE,
    saml_id                 VARCHAR                     UNIQUE,
    reference_id            VARCHAR                     UNIQUE,
    pseudonym               VARCHAR,
    created                 TIMESTAMP WITH TIME ZONE    NOT NULL,
    valid_until             TIMESTAMP WITH TIME ZONE    NOT NULL
    );

CREATE INDEX IF NOT EXISTS authentication_session_id_idx ON authentication(session_id);
CREATE INDEX IF NOT EXISTS authentication_token_id_idx ON authentication(token_id);
CREATE INDEX IF NOT EXISTS authentication_saml_id_idx ON authentication(saml_id);
CREATE INDEX IF NOT EXISTS authentication_reference_id_idx ON authentication(reference_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON authentication TO ${APP_USER};
GRANT USAGE ON SEQUENCE authentication_id_seq TO ${APP_USER};
