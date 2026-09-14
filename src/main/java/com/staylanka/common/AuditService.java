package com.staylanka.common;

import jakarta.persistence.EntityManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** Audit writes participate in the business transaction; no entity FK survives a purge. */
@Service
public class AuditService {
    private final EntityManager em;
    public AuditService(EntityManager em) { this.em = em; }

    @Transactional
    public void record(BaseEntity entity, String action) {
        record(entity.getClass().getSimpleName().split("\\$")[0], entity.getId(), action, action);
    }

    @Transactional
    public void record(String type, Long id, String action, String summary) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "SYSTEM" : auth.getName();
        em.createNativeQuery("INSERT INTO audit_events(entity_type,entity_id,action,actor,summary,occurred_at) VALUES (?1,?2,?3,?4,?5,?6)")
                .setParameter(1, type).setParameter(2, id).setParameter(3, action)
                .setParameter(4, actor).setParameter(5, summary).setParameter(6, LocalDateTime.now()).executeUpdate();
    }
}
