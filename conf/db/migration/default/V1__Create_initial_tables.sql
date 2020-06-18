CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    created  TIMESTAMP   NOT NULL,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(60) NOT NULL
);

CREATE TYPE lang AS ENUM ('PYTHON', 'JAVASCRIPT');

CREATE TABLE robots
(
    id           BIGSERIAL PRIMARY KEY,
    created      TIMESTAMP   NOT NULL,
    user_id      SERIAL      NOT NULL REFERENCES users (id),
    name         VARCHAR(15) NOT NULL,
    dev_code     TEXT        NOT NULL,
    automatch    BOOL        NOT NULL,
    is_published BOOL        NOT NULL,
    rating       INT         NOT NULL,
    lang         lang        NOT NULL
);

CREATE TABLE published_robots
(
    id       BIGSERIAL PRIMARY KEY,
    robot_id SERIAL    NOT NULL REFERENCES robots (id),
    created  TIMESTAMP NOT NULL,
    code     TEXT      NOT NULL
);

CREATE TYPE winner AS ENUM ('R1', 'R2', 'DRAW');

CREATE TABLE battles
(
    id        BIGSERIAL PRIMARY KEY,
    created   TIMESTAMP NOT NULL,
    r1_id     SERIAL    NOT NULL REFERENCES robots (id),
    r2_id     SERIAL    NOT NULL REFERENCES robots (id),
    ranked    BOOL      NOT NULL,
    winner    winner    NOT NULL,
--  If `errored` and r1_won/r2_won, then the other robot errored. Otherwise, both errored.
    errored   BOOL      NOT NULL,
    r1_rating INT       NOT NULL,
    r2_rating INT       NOT NULL,
    r1_time   REAL      NOT NULL,
    r2_time   REAL      NOT NULL,
    data      TEXT      NOT NULL
);
