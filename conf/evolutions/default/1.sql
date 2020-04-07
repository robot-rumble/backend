-- Users schema

-- !Ups

CREATE TABLE users
(
    Id       SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL
);

-- !Downs

DROP TABLE users;
