package com.staylanka.security;
import com.staylanka.user.AppUserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

public class AccountStatusFilter extends OncePerRequestFilter {
    private final AppUserRepository users;
    public AccountStatusFilter(AppUserRepository users) {this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof AccountPrincipal principal
                && users.findByEmailIgnoreCase(auth.getName()).filter(principal::matches).isEmpty()) {
            SecurityContextHolder.clearContext();
            if(request.getSession(false)!=null)request.getSession(false).invalidate();
            response.sendRedirect(request.getContextPath()+"/login?expired");return;
        }
        chain.doFilter(request,response);
    }
}
