package org.example.web.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.service.CurrentUserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;

    @ModelAttribute
    public void addSharedAttributes(Model model, HttpSession session) {
        model.addAttribute("currentUser", currentUserService.currentUser(session));
        model.addAttribute("signedIn", currentUserService.currentUser(session).authenticated());
    }
}
