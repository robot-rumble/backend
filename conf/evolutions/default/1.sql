-- !Ups

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    created  TIMESTAMP   NOT NULL DEFAULT NOW(),
    bio      TEXT,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE robots
(
    id          SERIAL PRIMARY KEY,
    created     TIMESTAMP   NOT NULL DEFAULT NOW(),
    user_id     SERIAL      NOT NULL REFERENCES users (id),
    name        VARCHAR(15) NOT NULL,
    bio         TEXT,
    open_source BOOL        NOT NULL DEFAULT TRUE,
    automatch   BOOL        NOT NULL DEFAULT TRUE,
    code        TEXT        NOT NULL,
    rating      INT         NOT NULL DEFAULT 1000
);

CREATE TABLE matches
(
    id        SERIAL PRIMARY KEY,
    created   TIMESTAMP NOT NULL DEFAULT NOW(),
    r1_id     SERIAL    NOT NULL REFERENCES robots (id),
    r2_id     SERIAL    NOT NULL REFERENCES robots (id),
    ranked    BOOL      NOT NULL DEFAULT TRUE,
    r1_won    BOOL      NOT NULL,
    r1_rating INT       NOT NULL,
    r2_rating INT       NOT NULL,
    r1_time   REAL      NOT NULL,
    r2_time   REAL      NOT NULL,
    r1_logs   TEXT,
    r2_logs   TEXT,
    data      JSON      NOT NULL
);

-- !Downs

DROP TABLE matches;
DROP TABLE robots;
DROP TABLE users;
