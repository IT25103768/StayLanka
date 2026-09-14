package com.staylanka.common;

import com.staylanka.user.AppUser;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class NotificationService {
    private final EntityManager em;
    public NotificationService(EntityManager em) { this.em = em; }
    @Transactional
    public void send(AppUser recipient, String message, String link) {
        em.createNativeQuery("INSERT INTO notifications(recipient_id,message,link,created_at) VALUES (?1,?2,?3,?4)")
                .setParameter(1, recipient.getId()).setParameter(2, message)
                .setParameter(3, link).setParameter(4, LocalDateTime.now()).executeUpdate();
    }

    @Transactional
    public void operations(com.staylanka.user.Role role, String message, String link) {
        for(AppUser user : em.createQuery("select u from AppUser u where u.active=true and (u.role=:role or u.role=com.staylanka.user.Role.ADMIN)",AppUser.class).setParameter("role",role).getResultList()) send(user,message,link);
    }
}
