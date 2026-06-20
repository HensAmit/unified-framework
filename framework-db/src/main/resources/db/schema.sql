-- Demonstration schema. In a real project this would mirror the application's
-- tables; here it's a local schema so DB-validation can be shown end to end.
-- Written in MySQL-compatible dialect (H2 runs in MODE=MySQL now, MySQL later).

CREATE TABLE IF NOT EXISTS playlists (
    id          INT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    owner       VARCHAR(255) NOT NULL,
    track_count INT DEFAULT 0
);
