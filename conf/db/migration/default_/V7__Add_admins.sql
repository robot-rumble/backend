ALTER TABLE users
    ADD COLUMN admin BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE board_memberships
(
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT REFERENCES users (id),
    board_id BIGINT REFERENCES boards (id)
);
