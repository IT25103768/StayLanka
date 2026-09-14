ALTER TABLE notifications ADD COLUMN notification_key VARCHAR(190);
CREATE UNIQUE INDEX uk_notification_key ON notifications(notification_key);
