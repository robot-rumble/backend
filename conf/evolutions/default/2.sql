-- Users schema

-- !Ups

CREATE TABLE robots
(
    Id      SERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    code    TEXT        NOT NULL,
    user_id SERIAL      NOT NULL REFERENCES users (Id)
);

-- !Downs

DROP TABLE robots;
