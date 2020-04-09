-- !Ups

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    bio      TEXT,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE robots
(
    id          SERIAL PRIMARY KEY,
    user_id     SERIAL      NOT NULL REFERENCES users (id),
    name        VARCHAR(15) NOT NULL,
    bio         TEXT,
    open_source BOOL        NOT NULL,
    code        TEXT        NOT NULL,
    rating      INT         NOT NULL
);

CREATE TABLE robot_matches
(
    id            SERIAL PRIMARY KEY,
    red_robot_id  SERIAL  NOT NULL REFERENCES robots (id),
    blue_robot_id SERIAL  NOT NULL REFERENCES robots (id),
    red_won       BOOLEAN NOT NULL,
    data JSON NOT NULL
);

-- !Downs

DROP TABLE robot_matches;
DROP TABLE robots;
DROP TABLE users;
