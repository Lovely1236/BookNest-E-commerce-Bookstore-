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

@Controller
@RequiredArgsConstructor
public class BookController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping("/home")
    public String home(Model model) {
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> featured =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books/featured");
        if (featured.isEmpty()) {
            featured = books.stream()
                    .sorted(Comparator.comparingDouble(book -> -toDouble(book.get("rating"))))
                    .limit(4)
                    .toList();
        }
        model.addAttribute("featuredBooks", featured.stream().limit(4).toList());
        model.addAttribute("latestBooks", books.stream().limit(6).toList());
        model.addAttribute("bookCount", books.size());
        model.addAttribute("genreCount", books.stream()
                .map(book -> String.valueOf(book.getOrDefault("genre", "")))
                .filter(genre -> !genre.isBlank())
                .distinct()
                .count());
        return "home";
    }

    @GetMapping("/books")
    public String searchBooks(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String genre,
                              Model model) {
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

        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("selectedGenre", genre == null ? "" : genre);
        model.addAttribute("genres", allBooks.stream()
                .map(book -> String.valueOf(book.getOrDefault("genre", "")))
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList());
        return "books";
    }

    @GetMapping("/book/{id}")
    public String viewBook(@PathVariable Long id, Model model) {
        Map<String, Object> book = backendGateway.getObject(backendGateway.serviceUrls().book() + "/books/" + id);
        model.addAttribute("book", book);
        model.addAttribute("reviews",
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews/book/" + id));
        model.addAttribute("averageRating",
                backendGateway.getDouble(backendGateway.serviceUrls().review() + "/reviews/avg/" + id, 0.0));
        model.addAttribute("recommendedBooks",
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books").stream()
                        .filter(candidate -> !String.valueOf(candidate.get("bookId")).equals(String.valueOf(id)))
                        .limit(3)
                        .toList());
        return "book-detail";
    }

    @GetMapping("/featured")
    public String viewFeatured(Model model) {
        List<Map<String, Object>> books =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books/featured");
        if (books.isEmpty()) {
            books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        }
        model.addAttribute("books", books.stream().limit(6).toList());
        return "featured";
    }

    @GetMapping("/cart")
    public String shoppingCart(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        Map<String, Object> cart =
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getOrDefault("items", List.of()));
        model.addAttribute("cartTotal", backendGateway.getDouble(
                backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0));
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long bookId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to add items to your cart.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("quantity", quantity);
        try {
            backendGateway.postStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/add",
                    request);
            flash(redirectAttributes, "Book added to cart.", "success");
            return "redirect:/cart";
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/book/" + bookId;
        }
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long itemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to modify your cart.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.deleteStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId()
                            + "/remove/" + itemId);
            flash(redirectAttributes, "Item removed from cart.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long itemId,
                             @RequestParam int quantity,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to update your cart.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId()
                            + "/update/" + itemId + "?quantity=" + quantity,
                    null);
            flash(redirectAttributes, "Cart updated.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to modify your cart.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.deleteStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            flash(redirectAttributes, "Cart cleared.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/cart";
    }

    @PostMapping("/wishlist/add")
    public String addToWishlist(@RequestParam Long bookId,
                                @RequestParam String bookTitle,
                                @RequestParam(defaultValue = "0") double bookPrice,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to save items to your wishlist.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("bookTitle", bookTitle);
        request.put("bookPrice", bookPrice);
        try {
            backendGateway.postStrict(
                    backendGateway.serviceUrls().wishlist() + "/wishlist/add/" + currentUser.userId(),
                    request);
            flash(redirectAttributes, "Book saved to wishlist.", "success");
            return "redirect:/wishlist";
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/book/" + bookId;
        }
    }

    @PostMapping("/review/add")
    public String addReview(@RequestParam Long bookId,
                            @RequestParam int rating,
                            @RequestParam String comment,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to post a review.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("userId", currentUser.userId());
        request.put("rating", rating);
        request.put("comment", comment);
        request.put("verified", true);
        try {
            backendGateway.postStrict(backendGateway.serviceUrls().review() + "/reviews", request);
            flash(redirectAttributes, "Thanks for sharing your review.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/book/" + bookId;
    }

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return "redirect:/register";
        }
        Map<String, Object> cart =
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getOrDefault("items", List.of()));
        model.addAttribute("cartTotal", backendGateway.getDouble(
                backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0));
        return "checkout";
    }

    @GetMapping("/payment")
    public String paymentMode(Model model, HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            return "redirect:/register";
        }
        Map<String, Object> cart =
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        Map<String, Object> wallet =
                backendGateway.getObject(backendGateway.serviceUrls().wallet() + "/wallet/" + currentUser.walletId());
        if (wallet.isEmpty()) {
            List<Map<String, Object>> wallets = backendGateway.getList(backendGateway.serviceUrls().wallet() + "/wallet");
            wallet = wallets.isEmpty()
                    ? backendGateway.postForObject(backendGateway.serviceUrls().wallet() + "/wallet/create", null)
                    : wallets.get(0);
            currentUserService.rememberWalletId(session, toLong(wallet.get("walletId")));
        }
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getOrDefault("items", List.of()));
        model.addAttribute("cartTotal", backendGateway.getDouble(
                backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0));
        model.addAttribute("wallet", wallet);
        return "payment";
    }

    @PostMapping("/payment/cod")
    public String cashOnDelivery(HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to place orders.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> cart =
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        List<Map<String, Object>> items = castList(cart.get("items"));
        if (items.isEmpty()) {
            flash(redirectAttributes, "Your cart is empty.", "error");
            return "redirect:/cart";
        }

        try {
            for (Map<String, Object> item : items) {
                backendGateway.postStrict(
                        backendGateway.serviceUrls().order() + "/orders/place",
                        buildOrderPayload(currentUser, item, 0.0));
            }
            backendGateway.deleteStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            sendNotification(session, "ORDER", "Your cash-on-delivery order has been placed.");
            flash(redirectAttributes, "Order placed successfully.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/orders";
    }

    @PostMapping("/payment/pay")
    public String proceedToPay(HttpSession session,
                               RedirectAttributes redirectAttributes) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        if (!currentUser.authenticated()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in to make payments.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> cart =
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId());
        List<Map<String, Object>> items = castList(cart.get("items"));
        double total = backendGateway.getDouble(
                backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/total", 0.0);

        if (items.isEmpty()) {
            flash(redirectAttributes, "Your cart is empty.", "error");
            return "redirect:/cart";
        }

        try {
            backendGateway.postForObjectStrict(
                    backendGateway.serviceUrls().wallet() + "/wallet/pay/"
                            + currentUser.walletId() + "?amount=" + total,
                    null);

            for (Map<String, Object> item : items) {
                double lineAmount = toDouble(item.get("price")) * toDouble(item.get("quantity"));
                backendGateway.postStrict(
                        backendGateway.serviceUrls().order() + "/orders/online",
                        buildOrderPayload(currentUser, item, lineAmount));
            }

            backendGateway.deleteStrict(
                    backendGateway.serviceUrls().cart() + "/cart/" + currentUser.userId() + "/clear");
            sendNotification(session, "PAYMENT", "Wallet payment completed for your order.");
            flash(redirectAttributes, "Payment successful and order placed.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/payment";
        }
        return "redirect:/orders";
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
        redirectAttributes.addFlashAttribute("flashMessage", message);
        redirectAttributes.addFlashAttribute("flashType", type);
    }
}
