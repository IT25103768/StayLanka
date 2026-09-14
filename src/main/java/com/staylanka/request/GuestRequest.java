package com.staylanka.request;

import com.staylanka.common.BaseEntity;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.reservation.Reservation;
import com.staylanka.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "guest_requests")
public class GuestRequest extends BaseEntity {
    @Column(name = "request_reference", nullable = false, unique = true, length = 30)
    private String requestReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private AppUser assignedStaff;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(nullable = false) private boolean archived;
    public boolean isArchived() { return archived; }
    public void setArchived(boolean value) { archived = value; }
    public String getOwnerLabel() { return assignedStaff == null ? "Customer Relations queue" : assignedStaff.getEmail(); }

    protected GuestRequest() {
    }

    public GuestRequest(String requestReference, CustomerProfile customer, Reservation reservation,
                        String category, String subject, String description,
                        RequestType type, RequestPriority priority) {
        this.requestReference = requestReference;
        this.customer = customer;
        updateCustomerFields(reservation, category, subject, description, type, priority);
    }

    public void updateCustomerFields(Reservation reservation, String category, String subject,
                                     String description, RequestType type, RequestPriority priority) {
        this.reservation = reservation;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.type = type;
        this.priority = priority;
    }

    public void assign(AppUser staff) {
        this.assignedStaff = staff;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public void transitionTo(RequestStatus status, String resolution) {
        this.status = status;
        if (resolution != null && !resolution.isBlank()) {
            this.resolution = resolution;
        }
    }

    public String getRequestReference() { return requestReference; }
    public CustomerProfile getCustomer() { return customer; }
    public Reservation getReservation() { return reservation; }
    public AppUser getAssignedStaff() { return assignedStaff; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public RequestType getType() { return type; }
    public RequestPriority getPriority() { return priority; }
    public RequestStatus getStatus() { return status; }
    public String getResolution() { return resolution; }
}
