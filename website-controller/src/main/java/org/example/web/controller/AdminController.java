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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String adminDashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to access this page.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        List<Map<String, Object>> books =
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> orders =
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> reviews =
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews");

        model.addAttribute("bookCount", books.size());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("wishlistCount",
                backendGateway.getList(backendGateway.serviceUrls().wishlist() + "/wishlist").size());
        model.addAttribute("recentBooks", books.stream().limit(5).toList());
        model.addAttribute("recentOrders", orders.stream().limit(5).toList());
        return "admin-dashboard";
    }

    @GetMapping("/addBook")
    public String addBook() {
        return "add-book";
    }

    @PostMapping("/addBook")
    public String saveBook(@RequestParam String title,
                           @RequestParam String author,
                           @RequestParam(required = false) String isbn,
                           @RequestParam(required = false) String genre,
                           @RequestParam(required = false) String publisher,
                           @RequestParam double price,
                           @RequestParam int stock,
                           @RequestParam(defaultValue = "0") double rating,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) String coverImageUrl,
                           @RequestParam(required = false) String publishedDate,
                           RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to perform this action.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("author", author);
        request.put("isbn", isbn);
        request.put("genre", genre);
        request.put("publisher", publisher);
        request.put("price", price);
        request.put("stock", stock);
        request.put("rating", rating);
        request.put("description", description);
        request.put("coverImageUrl", coverImageUrl);
        request.put("publishedDate", publishedDate);

        try {
            backendGateway.postStrict(backendGateway.serviceUrls().book() + "/books", request);
            flash(redirectAttributes, "Book added successfully.", "success");
            return "redirect:/admin/inventory";
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
            return "redirect:/admin/addBook";
        }
    }

    @PostMapping("/deleteBook")
    public String deleteBook(@RequestParam Long id,
                             RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to perform this action.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().book() + "/books/" + id);
            flash(redirectAttributes, "Book deleted successfully.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/admin";
    }

    @GetMapping("/orders")
    public String manageOrders(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to access this page.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        model.addAttribute("orders",
                backendGateway.getList(backendGateway.serviceUrls().order() + "/orders"));
        return "admin-orders";
    }

    @PostMapping("/orders/status")
    public String changeOrderStatus(@RequestParam Long orderId,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to perform this action.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().order() + "/orders/status/" + orderId + "?status=" + status,
                    null);
            flash(redirectAttributes, "Order status updated.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/users")
    public String manageUsers(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            model.addAttribute("authRequired", true);
            return "admin-users";
        }
        StorefrontUser currentUser = currentUserService.currentUser(session);
        List<Map<String, Object>> users = currentUser.authenticated()
                ? backendGateway.getList(backendGateway.serviceUrls().auth() + "/users", currentUser.token())
                : List.of();
        model.addAttribute("users", users);
        model.addAttribute("authRequired", !currentUser.authenticated());
        return "admin-users";
    }

    @PostMapping("/suspend")
    public String suspendUser(RedirectAttributes redirectAttributes) {
        flash(redirectAttributes, "User suspension is not supported by the backend yet.", "error");
        return "redirect:/admin/users";
    }

    @GetMapping("/analytics")
    public String viewAnalytics(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to access this page.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
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
    public String viewAllReviews(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to access this page.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        model.addAttribute("reviews",
                backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews"));
        return "reviews";
    }

    @PostMapping("/review/moderate")
    public String moderateReview(@RequestParam Long reviewId,
                                 RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to perform this action.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().review() + "/reviews/" + reviewId);
            flash(redirectAttributes, "Review removed.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/admin/reviews";
    }

    @GetMapping("/inventory")
    public String viewInventory(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to access this page.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        model.addAttribute("books",
                backendGateway.getList(backendGateway.serviceUrls().book() + "/books"));
        return "inventory";
    }

    @PostMapping("/inventory/update")
    public String updateStock(@RequestParam Long id,
                              @RequestParam int stock,
                              RedirectAttributes redirectAttributes, HttpSession session) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please sign in as an admin to perform this action.");
            redirectAttributes.addFlashAttribute("flashType", "error");
            return "redirect:/register";
        }
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().book() + "/books/" + id + "/stock?stock=" + stock,
                    null);
            flash(redirectAttributes, "Stock updated successfully.", "success");
        } catch (BackendGatewayException ex) {
            flash(redirectAttributes, ex.getMessage(), "error");
        }
        return "redirect:/admin/inventory";
    }

    private boolean isAdmin(HttpSession session) {
        StorefrontUser currentUser = currentUserService.currentUser(session);
        return currentUser.authenticated() && currentUser.isAdmin();
    }

    private void flash(RedirectAttributes redirectAttributes, String message, String type) {
        redirectAttributes.addFlashAttribute("flashMessage", message);
        redirectAttributes.addFlashAttribute("flashType", type);
    }
}
