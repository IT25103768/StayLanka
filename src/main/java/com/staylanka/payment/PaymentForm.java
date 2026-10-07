package com.staylanka.payment;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public class PaymentForm {
 @NotNull private PaymentKind kind=PaymentKind.PAYMENT;
 @NotNull private PaymentMethod method=PaymentMethod.CASH;
 @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) private BigDecimal amount;
 @Size(max=100) private String externalReference;
 @Size(max=500) private String note;
 @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
 private String requestKey=UUID.randomUUID().toString();
 public PaymentKind getKind(){return kind;} public void setKind(PaymentKind v){kind=v;}
 public PaymentMethod getMethod(){return method;} public void setMethod(PaymentMethod v){method=v;}
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
 public String getNote(){return note;} public void setNote(String v){note=v;}
 public String getRequestKey(){return requestKey;} public void setRequestKey(String v){requestKey=v;}
}
