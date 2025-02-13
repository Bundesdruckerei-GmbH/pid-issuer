CREATE TABLE IF NOT EXISTS pools
(
    id                  BIGSERIAL           PRIMARY KEY,
    bits                INTEGER             NOT NULL,
    size                INTEGER             NOT NULL,
    name                VARCHAR(128)        NOT NULL UNIQUE
);

GRANT SELECT, INSERT, UPDATE ON pools TO ${APP_USER};
GRANT USAGE ON SEQUENCE pools_id_seq TO ${APP_USER};

CREATE TABLE IF NOT EXISTS lists
(
    id                  UUID                PRIMARY KEY,
    pool_id             BIGINT              NOT NULL REFERENCES pools(id),
    base_uri            VARCHAR(1024)       NOT NULL,
    bits                INTEGER             NOT NULL,
    size                INTEGER             NOT NULL,
    version             INTEGER             NOT NULL,
    data                BIT VARYING         NOT NULL
);

CREATE INDEX lists_pool_id_idx ON lists (pool_id);
GRANT SELECT, INSERT, UPDATE ON lists TO ${APP_USER};

CREATE TABLE IF NOT EXISTS list_indices
(
    id                  BIGSERIAL           PRIMARY KEY,
    list_id             UUID                NOT NULL REFERENCES lists(id),
    index               INTEGER             NOT NULL,
    CONSTRAINT uc_list_indices_list_id_index UNIQUE (list_id, index)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON list_indices TO ${APP_USER};
GRANT USAGE ON SEQUENCE list_indices_id_seq TO ${APP_USER};
