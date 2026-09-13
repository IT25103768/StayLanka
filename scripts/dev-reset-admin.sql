-- DEVELOPMENT ONLY.
-- Restores the local administrator login to:
--   email:    admin@staylanka.lk
--   password: Admin@12345
-- Do not use this known password in production.

INSERT INTO app_users (email, password_hash, role, active, created_at, updated_at)
VALUES (
    'admin@staylanka.lk',
    '$2y$10$F4mr9ltSj/PbqJ7eul3NaevF5k3pndEVGyDeSylhtEF6Bv6.UNole',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role = 'ADMIN',
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;
