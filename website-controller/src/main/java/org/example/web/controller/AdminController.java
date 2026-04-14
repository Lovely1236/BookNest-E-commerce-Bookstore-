package org.example.web.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.BackendGatewayException;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping
        public Map<String, Object> adminDashboard(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> reviews = backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews");
        int wishlistCount = backendGateway.getList(backendGateway.serviceUrls().wishlist() + "/wishlist").size();
        return Map.of(
            "bookCount", books.size(),
            "orderCount", orders.size(),
            "reviewCount", reviews.size(),
            "wishlistCount", wishlistCount,
            "recentBooks", books.stream().limit(5).toList(),
            "recentOrders", orders.stream().limit(5).toList()
        );
        }

    @GetMapping("/addBook")
    public Map<String, Object> addBook() {
        return Map.of("action", "addBook", "message", "Use POST /admin/addBook with book data to add a book.");
    }

    @PostMapping("/addBook")
    public Map<String, Object> saveBook(@RequestParam String title,
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
                                        HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to perform this action.");
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
            return Map.of("status", "success", "message", "Book added successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/deleteBook")
    public Map<String, Object> deleteBook(@RequestParam Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to perform this action.");
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().book() + "/books/" + id);
            return Map.of("status", "success", "message", "Book deleted successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/orders")
    public Map<String, Object> manageOrders(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        return Map.of("orders", backendGateway.getList(backendGateway.serviceUrls().order() + "/orders"));
    }

    @PostMapping("/orders/status")
    public Map<String, Object> changeOrderStatus(@RequestParam Long orderId,
                                                 @RequestParam String status,
                                                 HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to perform this action.");
        }
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().order() + "/orders/status/" + orderId + "?status=" + status,
                    null);
            return Map.of("status", "success", "message", "Order status updated.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/users")
    public Map<String, Object> manageUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("authRequired", true);
        }
        StorefrontUser currentUser = currentUserService.currentUser(session);
        List<Map<String, Object>> users = currentUser.authenticated()
                ? backendGateway.getList(backendGateway.serviceUrls().auth() + "/users", currentUser.token())
                : List.of();
        return Map.of("users", users, "authRequired", !currentUser.authenticated());
    }

    @PostMapping("/suspend")
    public Map<String, Object> suspendUser() {
        return Map.of("status", "error", "message", "User suspension is not supported by the backend yet.");
    }

    @GetMapping("/analytics")
    public Map<String, Object> viewAnalytics(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> reviews = backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews");
        int notifications = backendGateway.getList(backendGateway.serviceUrls().notification() + "/notifications").size();
        return Map.of("orders", orders.size(), "reviews", reviews.size(), "notifications", notifications);
    }

    // New API endpoints to support frontend admin dashboard
    @GetMapping("/analytics/stats")
    public Map<String, Object> analyticsStats(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> users = backendGateway.getList(backendGateway.serviceUrls().auth() + "/users");
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");

        double totalRevenue = orders.stream().mapToDouble(o -> {
            Object v = o.getOrDefault("total", o.get("amount"));
            if (v instanceof Number) return ((Number) v).doubleValue();
            try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ex) { return 0.0; }
        }).sum();

        int totalOrders = orders.size();
        int totalUsers = users.size();
        int totalBooks = books.size();

        // Placeholder growth values (could be computed with historical data)
        double revenueGrowth = 0.0;
        double orderGrowth = 0.0;

        return Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "totalUsers", totalUsers,
                "totalBooks", totalBooks,
                "revenueGrowth", revenueGrowth,
                "orderGrowth", orderGrowth
        );
    }

    @GetMapping("/analytics/sales")
    public Map<String, Object> analyticsSales(@RequestParam(defaultValue = "month") String period, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE;
        int days = switch (period.toLowerCase()) {
            case "week" -> 7;
            case "year" -> 365;
            default -> 30;
        };
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1);

        final Map<String, Object> zeroAgg = new LinkedHashMap<>();
        zeroAgg.put("amount", 0.0);
        zeroAgg.put("orders", 0);

        Map<String, Map<String, Object>> byDate = orders.stream().map(o -> {
            Object raw = o.getOrDefault("createdAt", o.get("date"));
            String dateStr = String.valueOf(raw == null ? today.toString() : raw).substring(0, 10);
            double amt = 0.0;
            Object v = o.getOrDefault("total", o.get("amount"));
            if (v instanceof Number) amt = ((Number) v).doubleValue();
            else try { amt = Double.parseDouble(String.valueOf(v)); } catch (Exception ignored) {}
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", dateStr);
            m.put("amount", amt);
            return m;
        }).filter(m -> {
            try {
                LocalDate d = LocalDate.parse((String) m.get("date"));
                return !d.isBefore(from) && !d.isAfter(today);
            } catch (Exception e) { return false; }
        }).collect(Collectors.groupingBy(m -> (String) m.get("date"), Collectors.reducing(
                zeroAgg,
                m -> {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("amount", ((Number) m.get("amount")).doubleValue());
                    mm.put("orders", 1);
                    return mm;
                },
                (a, b) -> {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    double aa = ((Number) a.get("amount")).doubleValue();
                    double bb = ((Number) b.get("amount")).doubleValue();
                    int oa = ((Number) a.get("orders")).intValue();
                    int ob = ((Number) b.get("orders")).intValue();
                    mm.put("amount", aa + bb);
                    mm.put("orders", oa + ob);
                    return mm;
                }
        )));

        List<Map<String, Object>> result = from.datesUntil(today.plusDays(1)).map(d -> {
            String ds = d.format(fmt);
            Map<String, Object> agg = byDate.containsKey(ds) ? byDate.get(ds) : new LinkedHashMap<>() {{ put("amount", 0.0); put("orders", 0); }};
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", ds);
            out.put("revenue", agg.get("amount"));
            out.put("orders", agg.get("orders"));
            return out;
        }).collect(Collectors.toList());

        return Map.of("period", period, "data", result);
    }

    @GetMapping("/analytics/top-books")
    public Map<String, Object> analyticsTopBooks(@RequestParam(defaultValue = "10") int limit, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        Map<String, Integer> counts = new java.util.HashMap<>();
        Map<String, Double> revenue = new java.util.HashMap<>();
        for (Map<String, Object> o : orders) {
            Object itemsObj = o.get("items");
            if (itemsObj instanceof List) {
                List<?> items = (List<?>) itemsObj;
                for (Object io : items) {
                    if (io instanceof Map) {
                        Map<?, ?> im = (Map<?, ?>) io;
                        Object idObj = im.containsKey("bookId") ? im.get("bookId") : im.get("id");
                        String id = String.valueOf(idObj == null ? "" : idObj);

                        Object qtyObj = im.containsKey("quantity") ? im.get("quantity") : im.getOrDefault("qty", 1);
                        int qty;
                        if (qtyObj instanceof Number) qty = ((Number) qtyObj).intValue();
                        else try { qty = Integer.parseInt(String.valueOf(qtyObj)); } catch (Exception ex) { qty = 1; }

                        Object priceObj = im.getOrDefault("price", 0);
                        double price;
                        if (priceObj instanceof Number) price = ((Number) priceObj).doubleValue();
                        else try { price = Double.parseDouble(String.valueOf(priceObj)); } catch (Exception ex) { price = 0.0; }

                        counts.put(id, counts.getOrDefault(id, 0) + qty);
                        revenue.put(id, revenue.getOrDefault(id, 0.0) + price * qty);
                    }
                }
            }
        }
        List<Map<String, Object>> sorted = counts.entrySet().stream()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bookId", e.getKey());
                m.put("soldCount", e.getValue());
                m.put("revenue", revenue.getOrDefault(e.getKey(), 0.0));
                return m;
            })
            .sorted((a, b) -> Double.compare(((Number) b.get("revenue")).doubleValue(), ((Number) a.get("revenue")).doubleValue()))
            .limit(limit)
            .collect(Collectors.toList());
        return Map.of("topBooks", sorted);
    }

    @GetMapping("/analytics/low-stock")
    public Map<String, Object> analyticsLowStock(@RequestParam(defaultValue = "10") int threshold, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> low = books.stream().filter(b -> {
            Object s = b.get("stock");
            int stock = s instanceof Number ? ((Number) s).intValue() : Integer.parseInt(String.valueOf(s == null ? "0" : s));
            return stock < threshold;
        }).map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", b.get("id"));
            m.put("title", b.get("title"));
            m.put("stock", b.get("stock"));
            return m;
        }).collect(Collectors.toList());
        return Map.of("lowStock", low);
    }

    @GetMapping("/reviews")
    public Map<String, Object> viewAllReviews(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        return Map.of("reviews", backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews"));
    }

    @PostMapping("/review/moderate")
    public Map<String, Object> moderateReview(@RequestParam Long reviewId, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to perform this action.");
        }
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().review() + "/reviews/" + reviewId);
            return Map.of("status", "success", "message", "Review removed.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/inventory")
    public Map<String, Object> viewInventory(HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to access this resource.");
        }
        return Map.of("books", backendGateway.getList(backendGateway.serviceUrls().book() + "/books"));
    }

    @PostMapping("/inventory/update")
    public Map<String, Object> updateStock(@RequestParam Long id,
                                           @RequestParam int stock,
                                           HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("error", "admin_required", "message", "Please sign in as an admin to perform this action.");
        }
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().book() + "/books/" + id + "/stock?stock=" + stock,
                    null);
            return Map.of("status", "success", "message", "Stock updated successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
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
