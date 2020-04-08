-- !Ups

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE robots
(
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    code    TEXT        NOT NULL,
    user_id SERIAL      NOT NULL REFERENCES users (id)
);

CREATE TABLE robot_matches
(
    id            SERIAL PRIMARY KEY,
    red_robot_id  SERIAL  NOT NULL REFERENCES robots (id),
    blue_robot_id SERIAL  NOT NULL REFERENCES robots (id),
    red_won       BOOLEAN NOT NULL,
    data          TEXT    NOT NULL
);

-- !Downs

DROP TABLE robot_matches;
DROP TABLE robots;
DROP TABLE users;
