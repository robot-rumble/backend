ALTER TABLE published_robots
    ADD COLUMN deviation DOUBLE PRECISION NOT NULL DEFAULT 100;

ALTER TABLE published_robots
    ADD COLUMN volatility DOUBLE PRECISION NOT NULL DEFAULT 1;