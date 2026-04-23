package org.example.web.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.BackendGatewayException;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of("redirect", "/home");
    }

    @GetMapping("/login")
    public Map<String, Object> loginPage() {
        return Map.of("redirect", "/register");
    }

    @GetMapping("/register")
    public Map<String, Object> register() {
        return Map.of("authLoginUrl", backendGateway.serviceUrls().auth() + "/oauth2/authorization/github");
    }

    @GetMapping("/callback")
    public Map<String, Object> oauthCallback(@RequestParam String token, HttpSession session) {
        try {
            Map<String, Object> profile = backendGateway.getObject(backendGateway.serviceUrls().auth() + "/me", "Bearer " + token);
            currentUserService.storeAuthenticatedUser(session, "Bearer " + token, profile);
            return Map.of("status", "success", "message", "Signed in via GitHub.");
        } catch (Exception ex) {
            return Map.of("status", "error", "message", "OAuth login failed.");
        }
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestParam String fullName,
                                        @RequestParam String email,
                                        @RequestParam String password,
                                        @RequestParam(required = false) Long mobile,
                                        HttpSession session) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("fullName", fullName);
        request.put("email", email);
        request.put("password", password);
        request.put("mobile", mobile);

        try {
            Map<String, Object> response =
                    backendGateway.postForObjectStrict(backendGateway.serviceUrls().auth() + "/register", request);
            rememberAuthenticatedUser(session, email, String.valueOf(response.getOrDefault("token", "")));
            return Map.of("status", "success", "message", String.valueOf(response.getOrDefault("message", "Account created successfully.")));
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String email,
                                     @RequestParam String password,
                                     HttpSession session) {
        try {
            Map<String, Object> response = backendGateway.postForObjectStrict(backendGateway.serviceUrls().auth() + "/login", Map.of("email", email, "password", password));
            rememberAuthenticatedUser(session, email, String.valueOf(response.getOrDefault("token", "")));
            return Map.of("status", "success", "message", "Signed in successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/profile")
    public Map<String, Object> viewProfile(HttpSession session) {
        StorefrontUser currentUser = refreshProfile(session);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", currentUser.fullName());
        profile.put("email", currentUser.email());
        profile.put("mobile", currentUser.mobile());
        profile.put("membership", currentUser.membership());
        profile.put("userId", currentUser.userId());
        profile.put("authenticated", currentUser.authenticated());
        return Map.of("profile", profile);
    }

    @PostMapping("/profile/update")
    public Map<String, Object> updateProfile(@RequestParam String newPassword,
                                             HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in before changing your password.");
        }
        try {
            backendGateway.putStrict(backendGateway.serviceUrls().auth() + "/change-password?userId=" + currentUser.userId() + "&newPassword=" + URLEncoder.encode(newPassword, StandardCharsets.UTF_8), null, currentUser.token());
            return Map.of("status", "success", "message", "Password updated successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/orders")
    public Map<String, Object> viewOrders(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        return Map.of("orders", backendGateway.getList(backendGateway.serviceUrls().order() + "/orders/user/" + currentUser.userId()));
    }

    @GetMapping("/wallet")
    public Map<String, Object> viewWallet(HttpSession session) {
        Map<String, Object> wallet = resolveWallet(session);
        Object walletId = wallet.get("walletId");
        List<Map<String, Object>> statements = List.of();
        if (walletId != null) {
            statements = backendGateway.getList(backendGateway.serviceUrls().wallet() + "/wallet/statements/" + walletId);
        }
        return Map.of("wallet", wallet, "statements", statements);
    }

    @PostMapping("/wallet/add")
    public Map<String, Object> addMoney(@RequestParam double amount, HttpSession session) {
        try {
            Map<String, Object> wallet = resolveWallet(session);
            Long walletId = toLong(wallet.get("walletId"));
            backendGateway.postForObjectStrict(backendGateway.serviceUrls().wallet() + "/wallet/addMoney/" + walletId + "?amount=" + amount, null);
            sendNotification(session, "WALLET", "Wallet topped up with Rs. " + amount + ".");
            return Map.of("status", "success", "message", "Wallet balance updated.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/notifications")
        public Map<String, Object> viewNotifications(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        return Map.of(
            "notifications", backendGateway.getList(backendGateway.serviceUrls().notification() + "/notifications/user/" + currentUser.userId()),
            "unreadCount", backendGateway.getString(backendGateway.serviceUrls().notification() + "/notifications/unread/" + currentUser.userId(), "0")
        );
        }

    @PostMapping("/notifications/read")
    public Map<String, Object> markNotificationRead(@RequestParam Long notificationId) {
        try {
            backendGateway.putStrict(backendGateway.serviceUrls().notification() + "/notifications/read/" + notificationId, null);
            return Map.of("status", "success", "message", "Notification marked as read.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllNotificationsRead(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        try {
            backendGateway.putStrict(backendGateway.serviceUrls().notification() + "/notifications/readAll/" + currentUser.userId(), null);
            return Map.of("status", "success", "message", "All notifications marked as read.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/wishlist")
    public Map<String, Object> viewWishlist(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        Map<String, Object> wishlist = backendGateway.getObject(backendGateway.serviceUrls().wishlist() + "/wishlist/" + currentUser.userId());
        Object books = wishlist.getOrDefault("books", List.of());
        return Map.of("wishlist", wishlist, "wishlistBooks", books);
    }

    @PostMapping("/wishlist/remove")
    public Map<String, Object> removeFromWishlist(@RequestParam Long itemId, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().wishlist() + "/wishlist/remove/" + currentUser.userId() + "/" + itemId);
            return Map.of("status", "success", "message", "Item removed from wishlist.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        currentUserService.logout(session);
        return Map.of("status", "success", "message", "Signed out successfully.");
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
        // no-op for REST mode
    }
}
