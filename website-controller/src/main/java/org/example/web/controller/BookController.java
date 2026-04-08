package org.example.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class BookController {

    private final BackendGateway backendGateway;

    @GetMapping("/home")
    public String home(Model model) {
        List<Map<String, Object>> books =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        model.addAttribute("featuredBooks", books.stream().limit(4).toList());
        model.addAttribute("bookCount", books.size());
        return "home";
    }

    @GetMapping("/books")
    public String searchBooks(Model model) {
        model.addAttribute("books",
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books"));
        return "books";
    }

    @GetMapping("/book/{id}")
    public String viewBook(@PathVariable Long id, Model model) {
        model.addAttribute("book",
                backendGateway.getObject(backendGateway.serviceUrls().book() + "/books/" + id));
        model.addAttribute("reviews",
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews/book/" + id));
        model.addAttribute("averageRating",
                backendGateway.getDouble(backendGateway.serviceUrls().review() + "/reviews/avg/" + id, 0.0));
        return "book-detail";
    }

    @GetMapping("/featured")
    public String viewFeatured(Model model) {
        List<Map<String, Object>> books =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        model.addAttribute("books", books.stream().limit(6).toList());
        return "featured";
    }

    @GetMapping("/cart")
    public String shoppingCart(Model model) {
        model.addAttribute("cart",
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/1"));
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long bookId,
                            @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", 1);
        request.put("productId", bookId);
        request.put("quantity", quantity);
        backendGateway.post(backendGateway.serviceUrls().cart() + "/cart/add", request);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId) {
        backendGateway.delete(backendGateway.serviceUrls().cart() + "/cart/remove/1/" + productId);
        return "redirect:/cart";
    }

    @PostMapping("/wishlist/add")
    public String addToWishlist(@RequestParam Long bookId,
                                @RequestParam String bookTitle,
                                @RequestParam(defaultValue = "0") double bookPrice) {
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("bookTitle", bookTitle);
        request.put("bookPrice", bookPrice);
        backendGateway.post(backendGateway.serviceUrls().wishlist() + "/wishlist/add/5001", request);
        return "redirect:/wishlist";
    }

    @PostMapping("/review/add")
    public String addReview(@RequestParam Long bookId,
                            @RequestParam int rating,
                            @RequestParam String comment) {
        Map<String, Object> request = new HashMap<>();
        request.put("bookId", bookId);
        request.put("userId", 1001);
        request.put("rating", rating);
        request.put("comment", comment);
        request.put("verified", true);
        backendGateway.post(backendGateway.serviceUrls().review() + "/reviews", request);
        return "redirect:/book/" + bookId;
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("cart",
                backendGateway.getObject(backendGateway.serviceUrls().cart() + "/cart/1"));
        return "checkout";
    }

    @GetMapping("/payment")
    public String paymentMode() {
        return "payment";
    }

    @PostMapping("/payment/cod")
    public String cashOnDelivery(@RequestParam Long bookId,
                                 @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> order = new HashMap<>();
        order.put("userId", 1);
        order.put("quantity", quantity);
        order.put("modeOfPayment", "COD");
        order.put("amountPaid", 0.0);
        order.put("address", Map.of(
                "customerId", 1,
                "fullName", "BookNest User",
                "mobileNumber", "9999999999",
                "flatNumber", "221B",
                "city", "Lucknow",
                "pincode", 226001,
                "state", "UP"));
        order.put("book", Map.of(
                "productId", bookId,
                "productName", "Selected Book"));
        backendGateway.post(backendGateway.serviceUrls().order() + "/orders/place", order);
        return "redirect:/orders";
    }

    @PostMapping("/payment/pay")
    public String proceedToPay(@RequestParam Long walletId,
                               @RequestParam double amount,
                               @RequestParam Long bookId,
                               @RequestParam(defaultValue = "1") int quantity) {
        backendGateway.post(backendGateway.serviceUrls().wallet() + "/wallet/pay/" + walletId + "?amount=" + amount, null);

        Map<String, Object> order = new HashMap<>();
        order.put("userId", 1);
        order.put("quantity", quantity);
        order.put("amountPaid", amount);
        order.put("address", Map.of(
                "customerId", 1,
                "fullName", "BookNest User",
                "mobileNumber", "9999999999",
                "flatNumber", "221B",
                "city", "Lucknow",
                "pincode", 226001,
                "state", "UP"));
        order.put("book", Map.of(
                "productId", bookId,
                "productName", "Selected Book"));
        backendGateway.post(backendGateway.serviceUrls().order() + "/orders/online", order);
        return "redirect:/orders";
    }
}
