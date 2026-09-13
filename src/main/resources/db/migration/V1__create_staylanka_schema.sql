CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_role CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))
);

CREATE TABLE customer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    address VARCHAR(255),
    nationality VARCHAR(80),
    identification_number VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_customer_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE RESTRICT
);

CREATE TABLE staff_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_staff_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_staff_profiles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE RESTRICT
);

CREATE TABLE room_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    capacity INT NOT NULL,
    bed_information VARCHAR(120) NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    amenities TEXT,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_room_types_name UNIQUE (name),
    CONSTRAINT ck_room_types_capacity CHECK (capacity > 0),
    CONSTRAINT ck_room_types_price CHECK (base_price >= 0)
);

CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL,
    room_type_id BIGINT NOT NULL,
    description TEXT,
    nightly_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_rooms_number UNIQUE (room_number),
    CONSTRAINT fk_rooms_type FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE RESTRICT,
    CONSTRAINT ck_rooms_price CHECK (nightly_price >= 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'INACTIVE'))
);
CREATE INDEX idx_rooms_type_status ON rooms(room_type_id, status);
CREATE INDEX idx_rooms_price ON rooms(nightly_price);

CREATE TABLE room_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    primary_image BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_room_images_path UNIQUE (storage_path),
    CONSTRAINT fk_room_images_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT
);
CREATE INDEX idx_room_images_room ON room_images(room_id, primary_image);

CREATE TABLE promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    minimum_nights INT NOT NULL,
    minimum_amount DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_promotions_code UNIQUE (code),
    CONSTRAINT ck_promotions_type CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_promotions_value CHECK (discount_value > 0),
    CONSTRAINT ck_promotions_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_promotions_minimums CHECK (minimum_nights > 0 AND minimum_amount >= 0)
);
CREATE INDEX idx_promotions_active_dates ON promotions(active, start_date, end_date);

CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_reference VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_count INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    nightly_price_snapshot DECIMAL(12,2) NOT NULL,
    gross_total DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    promotion_id BIGINT,
    cancellation_reason VARCHAR(500),
    notes TEXT,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_reservations_reference UNIQUE (reservation_reference),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservations_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE RESTRICT,
    CONSTRAINT ck_reservations_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT ck_reservations_guest_count CHECK (guest_count > 0),
    CONSTRAINT ck_reservations_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'REJECTED', 'CHECKED_IN', 'CHECKED_OUT', 'NO_SHOW')),
    CONSTRAINT ck_reservations_amounts CHECK (nightly_price_snapshot >= 0 AND gross_total >= 0 AND discount_amount >= 0 AND total_amount >= 0)
);
CREATE INDEX idx_reservations_room_dates ON reservations(room_id, check_in_date, check_out_date, status);
CREATE INDEX idx_reservations_customer_created ON reservations(customer_id, created_at);
CREATE INDEX idx_reservations_status_date ON reservations(status, check_in_date);

CREATE TABLE promotion_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_promotion_usages_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_promotion_usages_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_promotion_usages_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
    CONSTRAINT ck_promotion_usages_amount CHECK (discount_amount >= 0)
);
CREATE INDEX idx_promotion_usages_promotion ON promotion_usages(promotion_id);

CREATE TABLE stays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    actual_check_in TIMESTAMP NOT NULL,
    actual_check_out TIMESTAMP,
    guest_count INT NOT NULL,
    notes TEXT,
    room_charge DECIMAL(12,2) NOT NULL,
    additional_charge_total DECIMAL(12,2) NOT NULL,
    final_total DECIMAL(12,2),
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_stays_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_stays_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_stays_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT,
    CONSTRAINT ck_stays_guest_count CHECK (guest_count > 0),
    CONSTRAINT ck_stays_amounts CHECK (room_charge >= 0 AND additional_charge_total >= 0 AND (final_total IS NULL OR final_total >= 0))
);
CREATE INDEX idx_stays_room_checkout ON stays(room_id, actual_check_out);

CREATE TABLE additional_charges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stay_id BIGINT NOT NULL,
    description VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_additional_charges_stay FOREIGN KEY (stay_id) REFERENCES stays(id) ON DELETE RESTRICT,
    CONSTRAINT ck_additional_charges_values CHECK (quantity > 0 AND unit_price >= 0 AND subtotal >= 0)
);
CREATE INDEX idx_additional_charges_stay ON additional_charges(stay_id);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stay_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_reviews_stay UNIQUE (stay_id),
    CONSTRAINT fk_reviews_stay FOREIGN KEY (stay_id) REFERENCES stays(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_reviews_status_created ON reviews(status, created_at);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);

CREATE TABLE guest_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_reference VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    reservation_id BIGINT,
    assigned_staff_id BIGINT,
    category VARCHAR(80) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    resolution TEXT,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_guest_requests_reference UNIQUE (request_reference),
    CONSTRAINT fk_guest_requests_customer FOREIGN KEY (customer_id) REFERENCES customer_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_guest_requests_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_guest_requests_staff FOREIGN KEY (assigned_staff_id) REFERENCES app_users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_guest_requests_type CHECK (type IN ('INQUIRY', 'SPECIAL_REQUEST')),
    CONSTRAINT ck_guest_requests_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_guest_requests_status CHECK (status IN ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'))
);
CREATE INDEX idx_guest_requests_customer ON guest_requests(customer_id, created_at);
CREATE INDEX idx_guest_requests_work_queue ON guest_requests(status, priority, updated_at);
CREATE INDEX idx_guest_requests_assigned_staff ON guest_requests(assigned_staff_id, status);

CREATE TABLE request_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_request_responses_request FOREIGN KEY (request_id) REFERENCES guest_requests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_responses_author FOREIGN KEY (author_id) REFERENCES app_users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_request_responses_request ON request_responses(request_id, created_at);

CREATE TABLE request_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    changed_by_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_request_history_request FOREIGN KEY (request_id) REFERENCES guest_requests(id) ON DELETE RESTRICT,
    CONSTRAINT fk_request_history_actor FOREIGN KEY (changed_by_id) REFERENCES app_users(id) ON DELETE RESTRICT
);
CREATE INDEX idx_request_history_request ON request_history(request_id, created_at);
