package org.example.web.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.BackendGatewayException;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping("/")
    public String home() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/register";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("authLoginUrl", backendGateway.serviceUrls().auth() + "/oauth2/authorization/github");
        return "register";
    }

    @GetMapping("/auth/callback")
    public String oauthCallback(@RequestParam String token, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> profile = backendGateway.getObject(backendGateway.serviceUrls().auth() + "/me", "Bearer " + token);
            currentUserService.storeAuthenticatedUser(session, "Bearer " + token, profile);
            flash(redirectAttributes, "Signed in via GitHub.", "success");
            return "redirect:/home";
        } catch (Exception ex) {
            flash(redirectAttributes, "OAuth login failed.", "error");
            return "redirect:/register";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) Long mobile,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("fullName", fullName);
        request.put("email", email);
        request.put("password", password);
        request.put("mobile", mobile);

        try {
            Map<String, Object> response =
                    backendGateway.postForObjectStrict(backendGateway.serviceUrls().auth() + "/register", request);
            rememberAuthenticatedUser(session, email, String.valueOf(response.getOrDefault("token", "")));
            flash(redirectAttributes,
                    String.valueOf(response.getOrDefault("message", "Account created successfully.")),
                    "success");
            return "redirect:/profile";
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> response = backendGateway.postForObjectStrict(
                    backendGateway.serviceUrls().auth() + "/login",
                    Map.of("email", email, "password", password));
            rememberAuthenticatedUser(session, email, String.valueOf(response.getOrDefault("token", "")));
            flash(redirectAttributes, "Signed in successfully.", "success");
            return "redirect:/home";
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/register";
        }
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        StorefrontUser currentUser = refreshProfile(session);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", currentUser.fullName());
        profile.put("email", currentUser.email());
        profile.put("mobile", currentUser.mobile());
        profile.put("membership", currentUser.membership());
        profile.put("userId", currentUser.userId());
        profile.put("authenticated", currentUser.authenticated());
        model.addAttribute("profile", profile);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String newPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            flash(redirectAttributes, "Please sign in before changing your password.", "error");
            return "redirect:/register";
        }

        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().auth() + "/change-password?userId="
                            + currentUser.userId() + "&newPassword="
                            + URLEncoder.encode(newPassword, StandardCharsets.UTF_8),
                    null,
                    currentUser.token());
            flash(redirectAttributes, "Password updated successfully.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/profile";
    }

    @GetMapping("/orders")
    public String viewOrders(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        model.addAttribute("orders",
                backendGateway.getList(
                        backendGateway.serviceUrls().order() + "/orders/user/" + currentUser.userId()));
        return "orders";
    }

    @GetMapping("/wallet")
    public String viewWallet(Model model, HttpSession session) {
        Map<String, Object> wallet = resolveWallet(session);
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

    @PostMapping("/wallet/add")
    public String addMoney(@RequestParam double amount,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> wallet = resolveWallet(session);
            Long walletId = toLong(wallet.get("walletId"));
            backendGateway.postForObjectStrict(
                    backendGateway.serviceUrls().wallet() + "/wallet/addMoney/" + walletId + "?amount=" + amount,
                    null);
            sendNotification(session, "WALLET", "Wallet topped up with Rs. " + amount + ".");
            flash(redirectAttributes, "Wallet balance updated.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/wallet";
    }

    @GetMapping("/notifications")
    public String viewNotifications(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        model.addAttribute("notifications",
                backendGateway.getList(
                        backendGateway.serviceUrls().notification() + "/notifications/user/" + currentUser.userId()));
        model.addAttribute("unreadCount",
                backendGateway.getString(
                        backendGateway.serviceUrls().notification() + "/notifications/unread/" + currentUser.userId(),
                        "0"));
        return "notifications";
    }

    @PostMapping("/notifications/read")
    public String markNotificationRead(@RequestParam Long notificationId,
                                       RedirectAttributes redirectAttributes) {
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().notification() + "/notifications/read/" + notificationId,
                    null);
            flash(redirectAttributes, "Notification marked as read.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().notification() + "/notifications/readAll/" + currentUser.userId(),
                    null);
            flash(redirectAttributes, "All notifications marked as read.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/notifications";
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        Map<String, Object> wishlist =
                backendGateway.getObject(
                        backendGateway.serviceUrls().wishlist() + "/wishlist/" + currentUser.userId());
        Object books = wishlist.getOrDefault("books", List.of());
        model.addAttribute("wishlist", wishlist);
        model.addAttribute("wishlistBooks", books);
        return "wishlist";
    }

    @PostMapping("/wishlist/remove")
    public String removeFromWishlist(@RequestParam Long itemId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        try {
            backendGateway.deleteStrict(
                    backendGateway.serviceUrls().wishlist() + "/wishlist/remove/"
                            + currentUser.userId() + "/" + itemId);
            flash(redirectAttributes, "Item removed from wishlist.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/wishlist";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        currentUserService.logout(session);
        flash(redirectAttributes, "Signed out successfully.", "success");
        return "redirect:/home";
    }

    private void rememberAuthenticatedUser(HttpSession session, String email, String token) {
        Map<String, Object> profile = backendGateway.getObject(
                backendGateway.serviceUrls().auth() + "/profile?email="
                        + URLEncoder.encode(email, StandardCharsets.UTF_8),
                token);
        if (profile.isEmpty()) {
            profile = Map.of("email", email);
        }
        currentUserService.storeAuthenticatedUser(session, token, profile);
    }

    private StorefrontUser refreshProfile(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (currentUser.authenticated()) {
            Map<String, Object> profile = backendGateway.getObject(
                    backendGateway.serviceUrls().auth() + "/profile?email="
                            + URLEncoder.encode(currentUser.email(), StandardCharsets.UTF_8),
                    currentUser.token());
            currentUserService.updateProfile(session, profile);
        }
        return currentUserService.currentUser(session);
    }

    private Map<String, Object> resolveWallet(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        Map<String, Object> wallet = backendGateway.getObject(
                backendGateway.serviceUrls().wallet() + "/wallet/" + currentUser.walletId());
        if (!wallet.isEmpty()) {
            return wallet;
        }

        List<Map<String, Object>> wallets = backendGateway.getList(backendGateway.serviceUrls().wallet() + "/wallet");
        if (!wallets.isEmpty()) {
            Map<String, Object> existing = wallets.get(0);
            currentUserService.rememberWalletId(session, toLong(existing.get("walletId")));
            return existing;
        }

        Map<String, Object> created =
                backendGateway.postForObjectStrict(backendGateway.serviceUrls().wallet() + "/wallet/create", null);
        currentUserService.rememberWalletId(session, toLong(created.get("walletId")));
        return created;
    }

    private void sendNotification(HttpSession session, String type, String message) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        backendGateway.post(
                backendGateway.serviceUrls().notification() + "/notifications",
                Map.of("userId", currentUser.userId(), "type", type, "message", message));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private void flash(RedirectAttributes redirectAttributes, String message, String type) {
        redirectAttributes.addFlashAttribute("flashMessage", message);
        redirectAttributes.addFlashAttribute("flashType", type);
    }
}
