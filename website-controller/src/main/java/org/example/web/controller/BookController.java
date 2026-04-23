package org.example.web.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.BackendGatewayException;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

        @GetMapping("/home")
        public Map<String, Object> home() {
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> featured = backendGateway.getList(backendGateway.serviceUrls().book() + "/books/featured");
        if (featured.isEmpty()) {
            featured = books.stream()
                .sorted(Comparator.comparingDouble(book -> -toDouble(book.get("rating"))))
                .limit(4)
                .toList();
        }
        return Map.of(
            "featuredBooks", featured.stream().limit(4).toList(),
            "latestBooks", books.stream().limit(6).toList(),
            "bookCount", books.size(),
            "genreCount", books.stream()
                .map(book -> String.valueOf(book.getOrDefault("genre", "")))
                .filter(genre -> !genre.isBlank())
                .distinct()
                .count()
        );
        }

    @GetMapping("/books")
        public Map<String, Object> searchBooks(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String genre) {
        List<Map<String, Object>> allBooks =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> books = allBooks;
        if (keyword != null && !keyword.isBlank()) {
            books = backendGateway.getList(
                    backendGateway.serviceUrls().book() + "/books/search?keyword="
                            + UriUtils.encodeQueryParam(keyword, StandardCharsets.UTF_8));
        } else if (genre != null && !genre.isBlank()) {
            books = backendGateway.getList(
                    backendGateway.serviceUrls().book() + "/books/genre/"
                            + UriUtils.encodePathSegment(genre, StandardCharsets.UTF_8));
        }

        return Map.of(
            "books", books,
            "keyword", keyword == null ? "" : keyword,
            "selectedGenre", genre == null ? "" : genre,
            "genres", allBooks.stream()
                .map(book -> String.valueOf(book.getOrDefault("genre", "")))
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList()
        );
    }

    @GetMapping("/book/{id}")
        public Map<String, Object> viewBook(@PathVariable Long id) {
        Map<String, Object> book = backendGateway.getObject(backendGateway.serviceUrls().book() + "/books/" + id);
        List<Map<String, Object>> reviews = backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews/book/" + id);
        double averageRating = backendGateway.getDouble(backendGateway.serviceUrls().review() + "/reviews/avg/" + id, 0.0);
        List<Map<String, Object>> recommendedBooks = backendGateway.getList(backendGateway.serviceUrls().book() + "/books").stream()
            .filter(candidate -> !String.valueOf(candidate.get("bookId")).equals(String.valueOf(id)))
            .limit(3)
            .toList();
        return Map.of("book", book, "reviews", reviews, "averageRating", averageRating, "recommendedBooks", recommendedBooks);
        }

    @GetMapping("/featured")
    public Map<String, Object> viewFeatured() {
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books/featured");
        if (books.isEmpty()) {
            books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        }
        return Map.of("books", books.stream().limit(6).toList());
    }

    @GetMapping("/cart")
    public Map<String, Object> shoppingCart(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        return Map.of("cart", cart, "cartItems", cart.getOrDefault("items", List.of()), "cartTotal", backendGateway.getDouble(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0));
    }

    @PostMapping("/cart/add")
    public Map<String, Object> addToCart(@RequestParam Long bookId,
                                         @RequestParam(defaultValue = "1") int quantity,
                                         HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to add items to your cart.");
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("quantity", quantity);
        try {
            backendGateway.postStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/add", request);
            return Map.of("status", "success", "message", "Book added to cart.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/cart/remove")
    public Map<String, Object> removeFromCart(@RequestParam Long itemId, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to modify your cart.");
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/remove/" + itemId);
            return Map.of("status", "success", "message", "Item removed from cart.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/cart/update")
    public Map<String, Object> updateCart(@RequestParam Long itemId,
                                          @RequestParam int quantity,
                                          HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to update your cart.");
        }
        try {
            backendGateway.putStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/update/" + itemId + "?quantity=" + quantity, null);
            return Map.of("status", "success", "message", "Cart updated.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/cart/clear")
    public Map<String, Object> clearCart(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to modify your cart.");
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            return Map.of("status", "success", "message", "Cart cleared.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/wishlist/add")
    public Map<String, Object> addToWishlist(@RequestParam Long bookId,
                                             @RequestParam String bookTitle,
                                             @RequestParam(defaultValue = "0") double bookPrice,
                                             HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to save items to your wishlist.");
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("bookTitle", bookTitle);
        request.put("bookPrice", bookPrice);
        try {
            backendGateway.postStrict(backendGateway.serviceUrls().wishlist() + "/wishlist/add/" + currentUser.userId(), request);
            return Map.of("status", "success", "message", "Book saved to wishlist.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/review/add")
    public Map<String, Object> addReview(@RequestParam Long bookId,
                                         @RequestParam int rating,
                                         @RequestParam String comment,
                                         HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to post a review.");
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("userId", currentUser.userId());
        request.put("rating", rating);
        request.put("comment", comment);
        request.put("verified", true);
        try {
            backendGateway.postStrict(backendGateway.serviceUrls().review() + "/reviews", request);
            return Map.of("status", "success", "message", "Thanks for sharing your review.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/checkout")
    public Map<String, Object> checkout(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to proceed to checkout.");
        }
        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        return Map.of("cart", cart, "cartItems", cart.getOrDefault("items", List.of()), "cartTotal", backendGateway.getDouble(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0));
    }

    @GetMapping("/payment")
    public Map<String, Object> paymentMode(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to access payment options.");
        }
        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        Map<String, Object> wallet = backendGateway.getObject(backendGateway.serviceUrls().wallet() + "/wallet/" + currentUser.walletId());
        if (wallet.isEmpty()) {
            List<Map<String, Object>> wallets = backendGateway.getList(backendGateway.serviceUrls().wallet() + "/wallet");
            wallet = wallets.isEmpty()
                    ? backendGateway.postForObject(backendGateway.serviceUrls().wallet() + "/wallet/create", null)
                    : wallets.get(0);
            currentUserService.rememberWalletId(session, toLong(wallet.get("walletId")));
        }
        return Map.of("cart", cart, "cartItems", cart.getOrDefault("items", List.of()), "cartTotal", backendGateway.getDouble(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0), "wallet", wallet);
    }

    @PostMapping("/payment/cod")
    public Map<String, Object> cashOnDelivery(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to place orders.");
        }
        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        List<Map<String, Object>> items = castList(cart.get("items"));
        if (items.isEmpty()) {
            return Map.of("status", "error", "message", "Your cart is empty.");
        }

        try {
            for (Map<String, Object> item : items) {
                backendGateway.postStrict(backendGateway.serviceUrls().order() + "/orders/place", buildOrderPayload(currentUser, item, 0.0));
            }
            backendGateway.deleteStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            sendNotification(session, "ORDER", "Your cash-on-delivery order has been placed.");
            return Map.of("status", "success", "message", "Order placed successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/payment/pay")
    public Map<String, Object> proceedToPay(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return Map.of("error", "authentication_required", "message", "Please sign in to make payments.");
        }
        Map<String, Object> cart = backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        List<Map<String, Object>> items = castList(cart.get("items"));
        double total = backendGateway.getDouble(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0);

        if (items.isEmpty()) {
            return Map.of("status", "error", "message", "Your cart is empty.");
        }

        try {
            backendGateway.postForObjectStrict(backendGateway.serviceUrls().wallet() + "/wallet/pay/" + currentUser.walletId() + "?amount=" + total, null);
            for (Map<String, Object> item : items) {
                double lineAmount = toDouble(item.get("price")) * toDouble(item.get("quantity"));
                backendGateway.postStrict(backendGateway.serviceUrls().order() + "/orders/online", buildOrderPayload(currentUser, item, lineAmount));
            }
            backendGateway.deleteStrict(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            sendNotification(session, "PAYMENT", "Wallet payment completed for your order.");
            return Map.of("status", "success", "message", "Payment successful and order placed.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    private Map<String, Object> buildOrderPayload(StorefrontUser currentUser,
                                                  Map<String, Object> item,
                                                  double amountPaid) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("userId", currentUser.userId());
        order.put("quantity", (int) toDouble(item.get("quantity")));
        order.put("amountPaid", amountPaid);
        order.put("address", Map.of(
                "customerId", currentUser.userId(),
                "fullName", currentUser.fullName(),
                "mobileNumber", String.valueOf(currentUser.mobile()),
                "flatNumber", "221B",
                "city", "Lucknow",
                "pincode", 226001,
                "state", "UP"));
        order.put("book", Map.of(
                "productId", toLong(item.get("bookId")),
                "productName", String.valueOf(item.getOrDefault("bookTitle", "BookNest Pick"))));
        return order;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return 0.0;
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

    private void sendNotification(HttpSession session, String type, String message) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        backendGateway.post(
                backendGateway.serviceUrls().notification() + "/notifications",
                Map.of("userId", currentUser.userId(), "type", type, "message", message));
    }

    private void flash(RedirectAttributes redirectAttributes, String message, String type) {
        // No-op in REST mode
    }
}
