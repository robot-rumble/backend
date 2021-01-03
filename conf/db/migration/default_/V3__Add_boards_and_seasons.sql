CREATE TABLE seasons
(
    id     BIGSERIAL PRIMARY KEY,
    name   VARCHAR(30) NOT NULL,
    slug   VARCHAR(30) NOT NULL,
    bio    TEXT        NOT NULL,
    start  TIMESTAMP   NOT NULL,
    end_ TIMESTAMP   NOT NULL
);

CREATE TABLE boards
(
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(30) NOT NULL,
    bio                  TEXT,
    season_id            BIGINT REFERENCES seasons (id),
    admin_id             BIGINT REFERENCES users (id),
    password             VARCHAR(30),
    publishing_enabled   BOOL        NOT NULL,
    matchmaking_enabled  BOOL        NOT NULL,
    publish_cooldown     INT         NOT NULL,
    publish_battle_num   INT         NOT NULL,
    recurrent_cooldown   INT         NOT NULL,
    recurrent_battle_num INT         NOT NULL
);

ALTER TABLE published_robots
    ADD COLUMN r_id BIGINT NOT NULL REFERENCES robots (id) DEFAULT 0;
ALTER TABLE published_robots
    ADD COLUMN board_id BIGINT NOT NULL REFERENCES boards (id) DEFAULT 0;
ALTER TABLE robots
    DROP COLUMN pr_id;
ALTER TABLE robots
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE battles
    ADD COLUMN board_id BIGINT NOT NULL REFERENCES boards (id) DEFAULT 0;

ALTER TABLE published_robots
    ADD COLUMN rating INT NOT NULL DEFAULT 1000;
ALTER TABLE robots
    DROP COLUMN rating;
ALTER TABLE battles
    RENAME COLUMN r1_rating to pr1_rating;
ALTER TABLE battles
    RENAME COLUMN r2_rating to pr2_rating;
ALTER TABLE battles
    RENAME COLUMN r1_rating_change to pr1_rating_change;
ALTER TABLE battles
    RENAME COLUMN r2_rating_change to pr2_rating_change;
