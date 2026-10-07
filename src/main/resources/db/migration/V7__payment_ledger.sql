CREATE TABLE payment_entries (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 reservation_id BIGINT NOT NULL,
 reference VARCHAR(40) NOT NULL UNIQUE,
 request_key VARCHAR(36) NOT NULL UNIQUE,
 kind VARCHAR(20) NOT NULL,
 method VARCHAR(30) NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 external_reference VARCHAR(100),
 note VARCHAR(500),
 recorded_by VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL,
 CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
 CONSTRAINT ck_payment_amount CHECK (amount > 0),
 CONSTRAINT ck_payment_kind CHECK (kind IN ('PAYMENT','REFUND')),
 CONSTRAINT ck_payment_method CHECK (method IN ('CASH','BANK_TRANSFER','CARD_TERMINAL'))
);
CREATE INDEX idx_payment_reservation ON payment_entries(reservation_id);
