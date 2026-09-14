CREATE TABLE maintenance_blocks (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, room_id BIGINT NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL,
 reason VARCHAR(500) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT fk_maintenance_room FOREIGN KEY(room_id) REFERENCES rooms(id),
 CONSTRAINT ck_maintenance_dates CHECK(end_date > start_date)
);
CREATE INDEX idx_maintenance_dates ON maintenance_blocks(room_id,active,start_date,end_date);
