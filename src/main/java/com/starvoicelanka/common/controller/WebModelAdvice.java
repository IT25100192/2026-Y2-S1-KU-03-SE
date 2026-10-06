package com.starvoicelanka.common.controller;

import com.starvoicelanka.common.helper.WebHelpers;
import com.starvoicelanka.config.AppProperties;
import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class WebModelAdvice {

    private final UserRepository userRepository;
    private final WebHelpers helpers;
    private final AppProperties properties;

    public WebModelAdvice(UserRepository userRepository, WebHelpers helpers, AppProperties properties) {
        this.userRepository = userRepository;
        this.helpers = helpers;
        this.properties = properties;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return userRepository.findByEmailIgnoreCase(ud.getUsername()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && user.getRole() == Role.ADMIN;
    }

    @ModelAttribute("isSponsorDesk")
    public boolean isSponsorDesk(Authentication authentication) {
        User user = currentUser(authentication);
        return user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.SPONSOR_MANAGER);
    }

    @ModelAttribute("helpers")
    public WebHelpers helpers() {
        return helpers;
    }

    @ModelAttribute("freeVotesPerRound")
    public int freeVotesPerRound() {
        return properties.getFreeVotesPerRound();
    }

    @ModelAttribute("votePriceLKR")
    public double votePriceLKR() {
        return properties.getVotePriceLKR();
    }
}
