package com.staylanka.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies shared layout state to all MVC views.
 *
 * The operations rail belongs only to the hotel's internal operations area.
 * Public guest-facing pages keep the full-width hospitality layout even when
 * an administrator or staff member happens to be signed in.
 */
@ControllerAdvice
public class LayoutModelAdvice {

    @ModelAttribute("operationsPage")
    public boolean operationsPage(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null
                && !contextPath.isBlank()
                && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return path.equals("/dashboard")
                || path.equals("/staff")
                || path.startsWith("/staff/")
                || path.equals("/admin")
                || path.startsWith("/admin/");
    }
}
