package com.staylanka.common;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReminderService {
    private final EntityManager em;
    @Value("${staylanka.reminders.enabled:true}") private boolean enabled;
    @Value("${staylanka.reminders.days-before:1}") private int daysBefore;
    public ReminderService(EntityManager em){this.em=em;}
    @Scheduled(fixedDelayString="${staylanka.reminders.interval-ms:3600000}",initialDelayString="${staylanka.reminders.interval-ms:3600000}")
    @Transactional public void deliver(){
        if(!enabled)return;
        em.createNativeQuery("""
            INSERT INTO notifications(recipient_id,message,link,created_at,notification_key)
            SELECT c.user_id,CONCAT('Upcoming stay: ',r.reservation_reference),CONCAT('/customer/reservations/',r.id),?1,CONCAT('arrival-',r.id,'-',r.check_in_date)
            FROM reservations r JOIN customer_profiles c ON c.id=r.customer_id
            WHERE r.status='CONFIRMED' AND r.check_in_date=?2
            AND NOT EXISTS(SELECT 1 FROM notifications n WHERE n.notification_key=CONCAT('arrival-',r.id,'-',r.check_in_date))
            """).setParameter(1,LocalDateTime.now()).setParameter(2,LocalDate.now().plusDays(Math.max(0,daysBefore))).executeUpdate();
    }
}
