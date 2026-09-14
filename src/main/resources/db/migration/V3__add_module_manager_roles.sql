ALTER TABLE app_users DROP CHECK ck_app_users_role;
ALTER TABLE app_users MODIFY role VARCHAR(40) NOT NULL;
ALTER TABLE app_users ADD CONSTRAINT ck_app_users_role CHECK (
    role IN (
        'CUSTOMER',
        'ROOM_MANAGER',
        'RESERVATION_MANAGER',
        'PROFILE_MANAGER',
        'STAY_MANAGER',
        'PROMOTION_REVIEW_MANAGER',
        'INQUIRY_REQUEST_MANAGER',
        'STAFF',
        'ADMIN'
    )
);
