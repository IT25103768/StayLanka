package com.staylanka.request;

import com.staylanka.user.AppUser;

public record RequestStatusChangedEvent(GuestRequest request, AppUser actor,
                                        RequestStatus oldStatus, RequestStatus newStatus, String note) {
}

