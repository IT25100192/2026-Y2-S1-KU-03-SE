package com.starvoicelanka.web;

import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.user.dto.UserDtos;
import com.starvoicelanka.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthWebController {

    private final UserService userService;
    private final AppProperties appProperties;

    public AuthWebController(UserService userService, AppProperties appProperties) {
        this.userService = userService;
        this.appProperties = appProperties;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(name = "next", required = false) String next,
            @RequestParam(name = "error", required = false) Boolean error,
            @RequestParam(name = "logout", required = false) Boolean logout,
            Model model) {
        if (Boolean.TRUE.equals(error)) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (Boolean.TRUE.equals(logout)) {
            model.addAttribute("message", "You have been signed out");
        }
        model.addAttribute("title", "Sign in");
        model.addAttribute("next", next);
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("title", "Register");
        model.addAttribute("form", new UserDtos.RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            UserDtos.RegisterRequest form,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("ok", "Account created. Enter the verification code we sent you.");
            return "redirect:/verify?email=" + (form.getEmail() != null ? form.getEmail() : "");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            model.addAttribute("title", "Register");
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verify(
            @RequestParam(name = "email", required = false, defaultValue = "") String email,
            Model model) {
        model.addAttribute("title", "Verify mobile");
        model.addAttribute("email", email);
        if ("console".equalsIgnoreCase(appProperties.getNotifyProvider()) && !email.isBlank()) {
            String code = userService.getPendingVerificationCode(email);
            if (code != null) {
                model.addAttribute("devCode", code);
            }
        }
        return "auth/verify";
    }

    @PostMapping("/verify")
    public String verifySubmit(
            @RequestParam("email") String email,
            @RequestParam("code") String code,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            userService.verifyMobile(email, code);
            redirectAttributes.addFlashAttribute("ok", "Account verified. You can vote now.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("title", "Verify mobile");
            return "auth/verify";
        }
    }

    @PostMapping("/verify/resend")
    public String resendVerification(
            @RequestParam(name = "email", required = false, defaultValue = "") String email,
            RedirectAttributes redirectAttributes) {
        try {
            userService.resendVerificationCode(email);
        } catch (Exception ignored) {}
        redirectAttributes.addFlashAttribute("info", "If that account needs verifying, a new code has been sent.");
        return "redirect:/verify?email=" + email;
    }

    @GetMapping("/forgot")
    public String forgot(Model model) {
        model.addAttribute("title", "Forgotten password");
        return "auth/forgot";
    }

    @PostMapping("/forgot")
    public String forgotSubmit(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            userService.requestPasswordReset(email);
        } catch (Exception ignored) {}
        redirectAttributes.addFlashAttribute("info", "If that account exists, a reset code is on its way.");
        return "redirect:/reset?email=" + email;
    }

    @GetMapping("/reset")
    public String reset(
            @RequestParam(name = "email", required = false, defaultValue = "") String email,
            Model model) {
        model.addAttribute("title", "Reset password");
        model.addAttribute("email", email);
        return "auth/reset";
    }

    @PostMapping("/reset")
    public String resetSubmit(
            @RequestParam("email") String email,
            @RequestParam("code") String code,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            userService.confirmPasswordReset(email, code, newPassword);
            redirectAttributes.addFlashAttribute("ok", "Password changed. Sign in with your new password.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("title", "Reset password");
            return "auth/reset";
        }
    }
}
