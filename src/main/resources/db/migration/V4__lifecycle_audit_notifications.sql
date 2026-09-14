CREATE TABLE audit_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 entity_type VARCHAR(80) NOT NULL, entity_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL, actor VARCHAR(190) NOT NULL,
 summary VARCHAR(500) NOT NULL, occurred_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_entity ON audit_events(entity_type, entity_id, occurred_at);
CREATE TABLE notifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 recipient_id BIGINT NOT NULL, message VARCHAR(500) NOT NULL,
 link VARCHAR(255) NOT NULL, read_at TIMESTAMP, created_at TIMESTAMP NOT NULL,
 CONSTRAINT fk_notification_user FOREIGN KEY (recipient_id) REFERENCES app_users(id)
);
CREATE INDEX idx_notification_user ON notifications(recipient_id, created_at);
ALTER TABLE customer_profiles ADD COLUMN preferences VARCHAR(1000);
ALTER TABLE customer_profiles ADD COLUMN marketing_consent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE stays ADD COLUMN voided BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE stays ADD COLUMN identity_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE guest_requests ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE password_recovery (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 token_hash VARCHAR(64), requested_at TIMESTAMP NOT NULL,
 expires_at TIMESTAMP, used_at TIMESTAMP,
 CONSTRAINT uk_recovery_user UNIQUE(user_id),
 CONSTRAINT uk_recovery_token UNIQUE(token_hash),
 CONSTRAINT fk_recovery_user FOREIGN KEY(user_id) REFERENCES app_users(id)
);
