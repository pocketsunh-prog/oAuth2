-- Initial data: default admin user (password: admin123)
-- The password is BCrypt-encoded for "admin123"
MERGE INTO users (id, username, email, password, enabled, role, created_at, updated_at) VALUES (
    1,
    'admin',
    'admin@example.com',
    '$2a$10$p36zo/NWDPFSWdOa2ZGbm.wmN9UAQtdxSSnNzB8aXFSVee3BI5Lzq',
    true,
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Reset identity counter so new users don't conflict with seeded IDs
ALTER TABLE users ALTER COLUMN id RESTART WITH 100;

-- Insert default OAuth2 client (client_secret: frontend-secret)
MERGE INTO oauth2_clients (id, client_id, client_secret, client_name, redirect_uri, authorized_grant_types, scopes, enabled, created_at) VALUES (
    1,
    'frontend-client',
    '$2a$10$yZiNoOFeIUHDiADFu2oZ..LXJnr9wJy7Xy289DyQLqJB4f4/UQMzG',
    'Frontend React Client',
    'http://localhost:5173/callback',
    'authorization_code,refresh_token,password,client_credentials',
    'read,write',
    true,
    CURRENT_TIMESTAMP
);

ALTER TABLE oauth2_clients ALTER COLUMN id RESTART WITH 100;
