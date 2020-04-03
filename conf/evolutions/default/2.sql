-- Users schema

-- !Ups

CREATE TABLE robots
(
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    code    TEXT        NOT NULL,
    user_id SERIAL      NOT NULL REFERENCES users (id)
);

-- !Downs

DROP TABLE robots;
