package com.staylanka.auth;

import com.staylanka.common.ConflictException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (!registrationForm.getPassword().equals(registrationForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            registrationService.register(registrationForm);
        } catch (ConflictException ex) {
            bindingResult.rejectValue("email", "email.exists", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("success", "Account created. You can now sign in.");
        return "redirect:/login";
    }
}
