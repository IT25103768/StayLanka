package com.staylanka.request;

import com.staylanka.common.BaseEntity;
import com.staylanka.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_history")
public class RequestHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private GuestRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private AppUser changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private RequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private RequestStatus newStatus;

    @Column(length = 500)
    private String note;

    protected RequestHistory() {
    }

    public RequestHistory(GuestRequest request, AppUser changedBy, RequestStatus oldStatus,
                          RequestStatus newStatus, String note) {
        this.request = request;
        this.changedBy = changedBy;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.note = note;
    }

    public GuestRequest getRequest() { return request; }
    public AppUser getChangedBy() { return changedBy; }
    public RequestStatus getOldStatus() { return oldStatus; }
    public RequestStatus getNewStatus() { return newStatus; }
    public String getNote() { return note; }
}
