package com.staylanka.request;

import com.staylanka.common.BaseEntity;
import com.staylanka.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "request_responses")
public class RequestResponse extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private GuestRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    protected RequestResponse() {
    }

    public RequestResponse(GuestRequest request, AppUser author, String message) {
        this.request = request;
        this.author = author;
        this.message = message;
    }

    public GuestRequest getRequest() { return request; }
    public AppUser getAuthor() { return author; }
    public String getMessage() { return message; }
}

