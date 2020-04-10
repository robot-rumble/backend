-- !Ups

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    created  TIMESTAMP   NOT NULL DEFAULT current_timestamp,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE robots
(
    id          SERIAL PRIMARY KEY,
    created     TIMESTAMP   NOT NULL DEFAULT current_timestamp,
    modified    TIMESTAMP   NOT NULL DEFAULT current_timestamp,
    user_id     SERIAL      NOT NULL REFERENCES users (id),
    name        VARCHAR(15) NOT NULL,
    bio         TEXT,
    open_source BOOL        NOT NULL DEFAULT TRUE,
    automatch   BOOL        NOT NULL DEFAULT TRUE,
    code        TEXT        NOT NULL,
    rating      INT         NOT NULL DEFAULT 1000
);

CREATE OR REPLACE FUNCTION update_modified_column() RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.code <> OLD.code THEN
        NEW.modified = now();
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_modified_column_trigger
    BEFORE UPDATE
    ON robots
    FOR EACH ROW
EXECUTE PROCEDURE update_modified_column();


CREATE TYPE battle_outcome AS ENUM ('r1_won', 'r2_won', 'draw');

CREATE TABLE battles
(
    id        SERIAL PRIMARY KEY,
    created   TIMESTAMP      NOT NULL DEFAULT NOW(),
    r1_id     SERIAL         NOT NULL REFERENCES robots (id),
    r2_id     SERIAL         NOT NULL REFERENCES robots (id),
    ranked    BOOL           NOT NULL DEFAULT TRUE,
    outcome   battle_outcome NOT NULL,
--  If `errored` and r1_won/r2_won, then the other robot errored. Otherwise, both errored.
    errored   BOOL           NOT NULL,
    r1_rating INT            NOT NULL,
    r2_rating INT            NOT NULL,
    r1_time   REAL           NOT NULL,
    r2_time   REAL           NOT NULL,
    r1_logs   TEXT,
    r2_logs   TEXT,
    data      JSON           NOT NULL
);

-- !Downs

DROP TABLE IF EXISTS battles;
DROP TYPE IF EXISTS battle_outcome;
DROP TABLE IF EXISTS robots;
DROP TABLE IF EXISTS users;
