-- Users schema

-- !Ups

CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    username CHAR(50) NOT NULL,
    password CHAR(50) NOT NULL
);

-- !Downs

DROP TABLE users;
