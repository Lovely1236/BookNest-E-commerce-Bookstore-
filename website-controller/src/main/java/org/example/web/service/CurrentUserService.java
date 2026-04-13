package org.example.web.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.config.StorefrontProperties;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String USER_ID = "storefront.userId";
    private static final String EMAIL = "storefront.email";
    private static final String FULL_NAME = "storefront.fullName";
    private static final String MOBILE = "storefront.mobile";
    private static final String TOKEN = "storefront.token";
    private static final String WALLET_ID = "storefront.walletId";
    private static final String ROLE = "storefront.role";

    private final StorefrontProperties storefrontProperties;

    public StorefrontUser currentUser(HttpSession session) {
        String token = asString(session.getAttribute(TOKEN), "");
        return new StorefrontUser(
                asLong(session.getAttribute(USER_ID), storefrontProperties.userId()),
                asString(session.getAttribute(EMAIL), storefrontProperties.email()),
                asString(session.getAttribute(FULL_NAME), storefrontProperties.fullName()),
                asLong(session.getAttribute(MOBILE), storefrontProperties.mobile()),
                storefrontProperties.membership(),
                token,
            asLong(session.getAttribute(WALLET_ID), storefrontProperties.walletId()),
            asString(session.getAttribute(ROLE), ""),
            !token.isBlank()
        );
    }

    public void storeAuthenticatedUser(HttpSession session, String token, Map<String, Object> profile) {
        session.setAttribute(TOKEN, token);
        updateProfile(session, profile);
    }

    public void updateProfile(HttpSession session, Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return;
        }

        rememberIfPresent(session, USER_ID, profile.get("userId"));
        rememberIfPresent(session, EMAIL, profile.get("email"));
        rememberIfPresent(session, FULL_NAME, profile.get("fullName"));
        rememberIfPresent(session, MOBILE, profile.get("mobile"));
        rememberIfPresent(session, ROLE, profile.get("role"));
    }

    public void rememberWalletId(HttpSession session, Long walletId) {
        if (walletId != null) {
            session.setAttribute(WALLET_ID, walletId);
        }
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private void rememberIfPresent(HttpSession session, String key, Object value) {
        if (value != null) {
            session.setAttribute(key, value);
        }
    }

    private Long asLong(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
