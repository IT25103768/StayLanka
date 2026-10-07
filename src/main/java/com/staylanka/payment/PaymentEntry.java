package com.staylanka.payment;
import com.staylanka.common.BaseEntity;
import com.staylanka.reservation.Reservation;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(name="payment_entries")
public class PaymentEntry extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY, optional=false)
 @JoinColumn(name="reservation_id",nullable=false) private Reservation reservation;
 @Column(nullable=false,unique=true,length=40) private String reference;
 @Column(name="request_key",nullable=false,unique=true,length=36) private String requestKey;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentKind kind;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PaymentMethod method;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(name="external_reference",length=100) private String externalReference;
 @Column(length=500) private String note;
 @Column(name="recorded_by",nullable=false,length=255) private String recordedBy;
 protected PaymentEntry() {}
 public PaymentEntry(Reservation reservation, PaymentForm form, String actor) {
  this.reservation=reservation;this.kind=form.getKind();this.method=form.getMethod();
  this.amount=form.getAmount().setScale(2);this.requestKey=form.getRequestKey();
  this.reference=(kind==PaymentKind.REFUND?"SLR-":"SLP-")+UUID.randomUUID();
  this.externalReference=form.getExternalReference();this.note=form.getNote();this.recordedBy=actor;
 }
 public Reservation getReservation(){return reservation;}
 public String getReference(){return reference;}
 public String getRequestKey(){return requestKey;}
 public PaymentKind getKind(){return kind;}
 public PaymentMethod getMethod(){return method;}
 public BigDecimal getAmount(){return amount;}
 public String getExternalReference(){return externalReference;}
 public String getNote(){return note;}
 public String getRecordedBy(){return recordedBy;}
}
