ALTER TABLE robots
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE robots
    ADD COLUMN error_count INT NOT NULL DEFAULT 0;
