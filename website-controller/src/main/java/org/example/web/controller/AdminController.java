package org.example.web.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final BackendGateway backendGateway;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("bookCount",
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books").size());
        model.addAttribute("orderCount",
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders").size());
        model.addAttribute("reviewCount",
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews").size());
        model.addAttribute("wishlistCount",
                backendGateway.getList(backendGateway.serviceUrls().wishlist() + "/wishlist").size());
        return "admin-dashboard";
    }

    @GetMapping("/addBook")
    public String addBook() {
        return "add-book";
    }

    @PostMapping("/editBook")
    public String editBook() {
        return "redirect:/admin";
    }

    @PostMapping("/deleteBook")
    public String deleteBook(@RequestParam Long id) {
        backendGateway.delete(backendGateway.serviceUrls().book() + "/books/" + id);
        return "redirect:/admin";
    }

    @GetMapping("/orders")
    public String manageOrders(Model model) {
        model.addAttribute("orders",
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders"));
        return "admin-orders";
    }

    @PostMapping("/orders/status")
    public String changeOrderStatus(@RequestParam Long orderId,
                                    @RequestParam String status) {
        backendGateway.put(backendGateway.serviceUrls().order() + "/orders/status/" + orderId + "?status=" + status, null);
        return "redirect:/admin/orders";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", List.of(
                Map.of("userId", 1, "name", "Ananya", "status", "ACTIVE"),
                Map.of("userId", 2, "name", "Rohan", "status", "ACTIVE"),
                Map.of("userId", 3, "name", "Sara", "status", "SUSPENDED")));
        return "admin-users";
    }

    @PostMapping("/suspend")
    public String suspendUser() {
        return "redirect:/admin/users";
    }

    @GetMapping("/analytics")
    public String viewAnalytics(Model model) {
        List<Map<String, Object>> orders =
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> reviews =
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews");
        model.addAttribute("orders", orders.size());
        model.addAttribute("reviews", reviews.size());
        model.addAttribute("notifications",
                backendGateway.getList(backendGateway.serviceUrls().notification() + "/notifications").size());
        return "analytics";
    }

    @GetMapping("/reviews")
    public String viewAllReviews(Model model) {
        model.addAttribute("reviews",
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews"));
        return "reviews";
    }

    @PostMapping("/review/moderate")
    public String moderateReview(@RequestParam Long reviewId) {
        backendGateway.delete(backendGateway.serviceUrls().review() + "/reviews/" + reviewId);
        return "redirect:/admin/reviews";
    }

    @GetMapping("/inventory")
    public String viewInventory(Model model) {
        model.addAttribute("books",
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books"));
        return "inventory";
    }

    @PostMapping("/inventory/update")
    public String updateStock() {
        return "redirect:/admin/inventory";
    }
}
