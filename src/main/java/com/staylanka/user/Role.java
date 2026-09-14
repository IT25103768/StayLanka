package com.staylanka.user;

public enum Role {
    CUSTOMER,
    ROOM_MANAGER,
    RESERVATION_MANAGER,
    PROFILE_MANAGER,
    STAY_MANAGER,
    PROMOTION_REVIEW_MANAGER,
    INQUIRY_REQUEST_MANAGER,
    STAFF,
    ADMIN;

    public boolean isModuleManager() {
        return switch (this) {
            case ROOM_MANAGER, RESERVATION_MANAGER, PROFILE_MANAGER, STAY_MANAGER,
                    PROMOTION_REVIEW_MANAGER, INQUIRY_REQUEST_MANAGER -> true;
            default -> false;
        };
    }
}
