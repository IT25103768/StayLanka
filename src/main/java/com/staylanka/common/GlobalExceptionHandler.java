package com.staylanka.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException ex, HttpServletRequest request, Model model) {
        return error(model, 404, "Page not found", ex.getMessage(), request);
    }

    @ExceptionHandler({ConflictException.class, BusinessRuleException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(RuntimeException ex, HttpServletRequest request, Model model) {
        return error(model, 409, "Request could not be completed", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(HttpServletRequest request, Model model) {
        return error(model, 403, "Access denied", "You do not have permission to access this resource.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String uploadTooLarge(HttpServletRequest request, Model model) {
        return error(model, 413, "Upload too large", "Room images must be no larger than 2 MB.", request);
    }

    private String error(Model model, int status, String title, String message, HttpServletRequest request) {
        model.addAttribute("status", status);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
        return "error/error";
    }
}
