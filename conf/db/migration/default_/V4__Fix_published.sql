UPDATE
    robots
SET
    published = pr.id IS NOT NULL
FROM robots r LEFT JOIN published_robots pr ON pr.r_id = r.id;

