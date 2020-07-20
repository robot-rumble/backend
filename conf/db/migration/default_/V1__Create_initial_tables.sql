CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    created  TIMESTAMP   NOT NULL,
    email    VARCHAR(50) NOT NULL,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(60) NOT NULL
);


CREATE TABLE published_robots
(
    id      BIGSERIAL PRIMARY KEY,
    created TIMESTAMP NOT NULL,
    code    TEXT      NOT NULL
);

-- CREATE TYPE lang AS ENUM ('PYTHON', 'JAVASCRIPT');

CREATE TABLE robots
(
    id        BIGSERIAL PRIMARY KEY,
    created   TIMESTAMP   NOT NULL,
    user_id   BIGINT      NOT NULL REFERENCES users (id),
    pr_id     BIGINT REFERENCES published_robots (id),
    name      VARCHAR(15) NOT NULL,
    dev_code  TEXT        NOT NULL,
    automatch BOOL        NOT NULL,
    rating    INT         NOT NULL,
    lang      VARCHAR(10) NOT NULL
);

-- CREATE TYPE winner AS ENUM ('R1', 'R2', 'DRAW');

CREATE TABLE battles
(
    id        BIGSERIAL PRIMARY KEY,
    created   TIMESTAMP  NOT NULL,
    r1_id     BIGINT     NOT NULL REFERENCES robots (id),
    r2_id     BIGINT     NOT NULL REFERENCES robots (id),
    pr1_id    BIGINT     NOT NULL REFERENCES published_robots (id),
    pr2_id    BIGINT     NOT NULL REFERENCES published_robots (id),
    ranked    BOOL       NOT NULL,
    winner    VARCHAR(5) NOT NULL,
--  If `errored` and r1_won/r2_won, then the other robot errored. Otherwise, both errored.
    errored   BOOL       NOT NULL,
    r1_rating INT        NOT NULL,
    r2_rating INT        NOT NULL,
    r1_time   REAL       NOT NULL,
    r2_time   REAL       NOT NULL,
    data      TEXT       NOT NULL
);

CREATE TABLE password_reset_tokens
(
    id      BIGSERIAL PRIMARY KEY,
    created TIMESTAMP   NOT NULL,
    token   VARCHAR(15) NOT NULL,
    user_id BIGINT REFERENCES users (id)
);
