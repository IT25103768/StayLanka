package com.staylanka.auth;

import com.staylanka.common.*;
import com.staylanka.user.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Base64;

/** Manual verified delivery: never logs or claims to email a reset token. */
@Service
public class RecoveryService {
    private final EntityManager em;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    public RecoveryService(EntityManager em, AppUserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.em=em; this.users=users; this.encoder=encoder; this.audit=audit;
    }
    @Transactional
    public void request(String email) {
        if(email==null || email.length()>190) return;
        users.findByEmailIgnoreCase(email.trim()).filter(AppUser::isActive).ifPresent(user -> {
            em.lock(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            var found=em.createNativeQuery("SELECT id FROM password_recovery WHERE user_id=?1").setParameter(1,user.getId()).getResultList();
            if(found.isEmpty()) em.createNativeQuery("INSERT INTO password_recovery(user_id,requested_at) VALUES (?1,?2)")
                    .setParameter(1,user.getId()).setParameter(2,LocalDateTime.now()).executeUpdate();
            else em.createNativeQuery("UPDATE password_recovery SET requested_at=?1,used_at=NULL,token_hash=NULL,expires_at=NULL WHERE user_id=?2 AND used_at IS NOT NULL")
                    .setParameter(1,LocalDateTime.now()).setParameter(2,user.getId()).executeUpdate();
        });
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public String issue(Long userId, boolean verified) {
        if(!verified) throw new BusinessRuleException("Verify identity through an independent, trusted channel first.");
        AppUser user=users.findById(userId).filter(AppUser::isActive).orElseThrow(() -> new NotFoundException("Active account not found."));
        em.lock(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        int changed=em.createNativeQuery("UPDATE password_recovery SET token_hash=?1,expires_at=?2,used_at=NULL WHERE user_id=?3")
                .setParameter(1,hash(token)).setParameter(2,LocalDateTime.now().plusMinutes(30)).setParameter(3,userId).executeUpdate();
        if(changed!=1) throw new BusinessRuleException("The account must request recovery first.");
        audit.record(user,"RESET_ISSUED"); return token;
    }
    @Transactional
    public void reset(String token, String password, String confirmation) {
        if(token==null || token.length()>100 || password==null || !password.equals(confirmation)
                || !password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,72}$") || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessRuleException("Use matching passwords of 8–72 bytes with a letter and number.");
        var rows=em.createNativeQuery("SELECT user_id FROM password_recovery WHERE token_hash=?1 AND used_at IS NULL AND expires_at>?2 FOR UPDATE")
                .setParameter(1,hash(token)).setParameter(2,LocalDateTime.now()).getResultList();
        if(rows.isEmpty()) throw new BusinessRuleException("The recovery token is invalid, expired or already used.");
        Long userId=((Number)rows.get(0)).longValue();
        AppUser user=users.findById(userId).filter(AppUser::isActive).orElseThrow(() -> new BusinessRuleException("Account unavailable."));
        user.setPasswordHash(encoder.encode(password));
        em.createNativeQuery("UPDATE password_recovery SET used_at=?1,token_hash=NULL WHERE user_id=?2")
                .setParameter(1,LocalDateTime.now()).setParameter(2,userId).executeUpdate();
        audit.record(user,"PASSWORD_RESET");
    }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
