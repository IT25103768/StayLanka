package com.staylanka.payment;
import java.math.BigDecimal;
import java.util.List;
public record PaymentSummary(BigDecimal total, BigDecimal received, BigDecimal refunded,
                             BigDecimal netPaid, BigDecimal balance, BigDecimal credit, String status) {
 public static PaymentSummary of(BigDecimal total,List<PaymentEntry> entries) {
  BigDecimal received=BigDecimal.ZERO,refunded=BigDecimal.ZERO;
  for(var e:entries) { if(e.getKind()==PaymentKind.PAYMENT) received=received.add(e.getAmount()); else refunded=refunded.add(e.getAmount()); }
  BigDecimal net=received.subtract(refunded),difference=total.subtract(net);
  return new PaymentSummary(total,received,refunded,net,difference.max(BigDecimal.ZERO),
   difference.negate().max(BigDecimal.ZERO),difference.signum()<0?"CREDIT":difference.signum()==0?"PAID":net.signum()>0?"PARTIALLY_PAID":"UNPAID");
 }
}
