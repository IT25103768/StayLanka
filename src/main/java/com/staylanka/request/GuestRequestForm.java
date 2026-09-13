package com.staylanka.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GuestRequestForm {
    private Long reservationId;
    @NotBlank @Size(max = 80)
    private String category;
    @NotBlank @Size(max = 180)
    private String subject;
    @NotBlank @Size(min = 10, max = 4000)
    private String description;
    @NotNull
    private RequestType type;
    @NotNull
    private RequestPriority priority = RequestPriority.MEDIUM;

    public static GuestRequestForm from(GuestRequest request) {
        GuestRequestForm form = new GuestRequestForm();
        form.reservationId = request.getReservation() == null ? null : request.getReservation().getId();
        form.category = request.getCategory();
        form.subject = request.getSubject();
        form.description = request.getDescription();
        form.type = request.getType();
        form.priority = request.getPriority();
        return form;
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }
    public RequestPriority getPriority() { return priority; }
    public void setPriority(RequestPriority priority) { this.priority = priority; }
}

