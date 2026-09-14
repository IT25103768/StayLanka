package com.staylanka.security;
import com.staylanka.user.AppUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

/** Retains a one-way credential fingerprint after Spring erases the login password. */
public class AccountPrincipal extends User {
    private final String fingerprint;
    public AccountPrincipal(AppUser account) {
        super(account.getEmail(),account.getPasswordHash(),account.isActive(),true,true,true,
                List.of(new SimpleGrantedAuthority("ROLE_"+account.getRole().name())));
        fingerprint=fingerprint(account.getPasswordHash());
    }
    public boolean matches(AppUser account) {
        return account.isActive() && fingerprint.equals(fingerprint(account.getPasswordHash()))
                && getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+account.getRole().name()));
    }
    private static String fingerprint(String hash) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(hash.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
