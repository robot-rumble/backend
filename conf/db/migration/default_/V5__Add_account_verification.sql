ALTER TABLE users
    ADD COLUMN verified BOOLEAN NOT NULL default true;

CREATE TABLE account_verifications
(
    id      BIGSERIAL PRIMARY KEY,
    token   VARCHAR(15) NOT NULL,
    user_id BIGINT REFERENCES users (id)
);
