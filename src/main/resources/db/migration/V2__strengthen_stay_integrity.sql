ALTER TABLE stays
    ADD CONSTRAINT ck_stays_checkout_after_checkin
    CHECK (actual_check_out IS NULL OR actual_check_out > actual_check_in);
