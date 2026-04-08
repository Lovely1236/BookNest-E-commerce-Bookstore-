package org.example.web.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final BackendGateway backendGateway;

    @GetMapping("/")
    public String home() {
        return "redirect:/home";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/login")
    public String login() {
        return "redirect:/home";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        model.addAttribute("profile", Map.of(
                "name", "BookNest User",
                "email", "reader@booknest.com",
                "membership", "Gold Reader"));
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile() {
        return "redirect:/profile";
    }

    @GetMapping("/orders")
    public String viewOrders(Model model) {
        model.addAttribute("orders",
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders/user/1"));
        return "orders";
    }

    @GetMapping("/wallet")
    public String viewWallet(Model model) {
        List<Map<String, Object>> wallets =
                backendGateway.getList(backendGateway.serviceUrls().wallet() + "/wallet");

        Map<String, Object> wallet = wallets.isEmpty()
                ? backendGateway.postForObject(backendGateway.serviceUrls().wallet() + "/wallet/create", null)
                : wallets.get(0);

        Object walletId = wallet.get("walletId");
        List<Map<String, Object>> statements = List.of();
        if (walletId != null) {
            statements = backendGateway.getList(
                    backendGateway.serviceUrls().wallet() + "/wallet/statements/" + walletId);
        }

        model.addAttribute("wallet", wallet);
        model.addAttribute("statements", statements);
        return "wallet";
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model) {
        model.addAttribute("notifications",
                backendGateway.getList(backendGateway.serviceUrls().notification() + "/notifications/user/3001"));
        model.addAttribute("unreadCount",
                backendGateway.getString(backendGateway.serviceUrls().notification() + "/notifications/unread/3001", "0"));
        return "notifications";
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Model model) {
        Map<String, Object> wishlist =
                backendGateway.getObject(backendGateway.serviceUrls().wishlist() + "/wishlist/5001");
        Object books = wishlist.getOrDefault("books", List.of());
        model.addAttribute("wishlist", wishlist);
        model.addAttribute("wishlistBooks", books);
        return "wishlist";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/home";
    }
}
