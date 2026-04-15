package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.BackendGateway;
import org.example.web.service.BackendGatewayException;
import org.example.web.service.CurrentUserService;
import org.example.web.service.StorefrontUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final BackendGateway backendGateway;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Map<String, Object> adminDashboard() {
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
                                        @RequestParam(required = false) String publishedDate ){
        
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
            backendGateway.postStrict(backendGateway.serviceUrls().book() + "/books", sanitizeBookPayload(request));
            return Map.of("status", "success", "message", "Book added successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/books")
    public Map<String, Object> manageBooks(@RequestParam(required = false) String search,
                                           @RequestParam(required = false) String genre,
                                           @RequestParam(defaultValue = "all") String stockStatus,
                                           @RequestParam(required = false) Integer limit) {
        

        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> filtered = books.stream()
                .filter(book -> matchesSearch(book, search))
                .filter(book -> matchesGenre(book, genre))
                .filter(book -> matchesStockStatus(book, stockStatus))
                .sorted((left, right) -> Long.compare(asLong(right.get("bookId"), 0L), asLong(left.get("bookId"), 0L)))
                .collect(Collectors.toList());

        if (limit != null && limit > 0 && filtered.size() > limit) {
            filtered = filtered.subList(0, limit);
        }

        long lowStockCount = books.stream()
                .filter(book -> {
                    int stock = asInt(book.get("stock"), 0);
                    return stock > 0 && stock <= LOW_STOCK_THRESHOLD;
                })
                .count();
        long outOfStockCount = books.stream()
                .filter(book -> asInt(book.get("stock"), 0) == 0)
                .count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("books", filtered);
        response.put("total", filtered.size());
        response.put("totalCount", books.size());
        response.put("lowStockCount", lowStockCount);
        response.put("outOfStockCount", outOfStockCount);
        return response;
    }

    @PostMapping("/books")
    public Map<String, Object> createBook(@RequestBody Map<String, Object> request){
                                         
        try {
            return backendGateway.postForMap(backendGateway.serviceUrls().book() + "/books", sanitizeBookPayload(request));
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PutMapping("/books/{id}")
    public Map<String, Object> updateBook(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request){
        
        try {
            return backendGateway.putForMap(backendGateway.serviceUrls().book() + "/books/" + id, sanitizeBookPayload(request));
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @DeleteMapping("/books/{id}")
    public Map<String, Object> deleteBook(@PathVariable Long id) {
       
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().book() + "/books/" + id);
            return Map.of("status", "success", "message", "Book deleted successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @PostMapping("/deleteBook")
    public Map<String, Object> deleteBookLegacy(@RequestParam Long id){
        return deleteBook(id);
    }

    @PatchMapping("/books/{id}/stock")
    public Map<String, Object> updateBookStock(@PathVariable Long id,
                                               @RequestBody Map<String, Object> request) {
        int stock = asInt(request.get("stock"), 0);
        try {
            return backendGateway.putForMap(
                    backendGateway.serviceUrls().book() + "/books/" + id + "/stock?stock=" + stock,
                    null
            );
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/orders")
    public Map<String, Object> manageOrders(@RequestParam(required = false) String status) {
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(order -> status.equalsIgnoreCase(asString(order.get("orderStatus"), "")))
                    .collect(Collectors.toList());
        }
        return Map.of("orders", orders, "total", orders.size());
    }

    @PostMapping("/orders/status")
    public Map<String, Object> changeOrderStatus(@RequestParam Long orderId,
                                                 @RequestParam String status) {
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
    public Map<String, Object> manageUsers() {
        List<Map<String, Object>> users = backendGateway.getList(backendGateway.serviceUrls().auth() + "/users");
        return Map.of("users", users);
    }

    @PostMapping("/suspend")
    public Map<String, Object> suspendUser() {
        return Map.of("status", "error", "message", "User suspension is not supported by the backend yet.");
    }

    @GetMapping("/analytics")
    public Map<String, Object> viewAnalytics() {
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> reviews = backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews");
        int notifications = backendGateway.getList(backendGateway.serviceUrls().notification() + "/notifications").size();
        return Map.of("orders", orders.size(), "reviews", reviews.size(), "notifications", notifications);
    }

    @GetMapping("/analytics/stats")
    public Map<String, Object> analyticsStats() {
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> users = backendGateway.getList(backendGateway.serviceUrls().auth() + "/users");
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");

        double totalRevenue = orders.stream()
                .mapToDouble(order -> asDouble(firstNonNull(order, "amountPaid", "total", "amount"), 0.0))
                .sum();

        int totalOrders = orders.size();
        int totalUsers = users.size();
        int totalBooks = books.size();

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
    public Map<String, Object> analyticsSales(@RequestParam(defaultValue = "month") String period) {
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE;
        int days = switch (period.toLowerCase()) {
            case "week" -> 7;
            case "year" -> 365;
            default -> 30;
        };
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1);

        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> order : orders) {
            String rawDate = asString(firstNonNull(order, "orderDate", "createdAt", "date"), today.toString());
            String dateStr = rawDate.length() >= 10 ? rawDate.substring(0, 10) : rawDate;
            LocalDate d;
            try {
                d = LocalDate.parse(dateStr);
            } catch (Exception ex) { continue; }
            if (d.isBefore(from) || d.isAfter(today)) continue;
            double amt = asDouble(firstNonNull(order, "amountPaid", "total", "amount"), 0.0);

            Map<String, Object> agg = byDate.computeIfAbsent(dateStr, k -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("amount", 0.0);
                mm.put("orders", 0);
                return mm;
            });
            double prevAmt = ((Number) agg.get("amount")).doubleValue();
            int prevOrders = ((Number) agg.get("orders")).intValue();
            agg.put("amount", prevAmt + amt);
            agg.put("orders", prevOrders + 1);
        }

        Map<String, Object> defaultAgg = new LinkedHashMap<>();
        defaultAgg.put("amount", 0.0);
        defaultAgg.put("orders", 0);

        List<Map<String, Object>> result = from.datesUntil(today.plusDays(1)).map(d -> {
            String ds = d.format(fmt);
            Map<String, Object> agg = byDate.containsKey(ds) ? byDate.get(ds) : defaultAgg;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", ds);
            out.put("revenue", agg.get("amount"));
            out.put("orders", agg.get("orders"));
            return out;
        }).collect(Collectors.toList());

        return Map.of("period", period, "data", result);
    }

    @GetMapping("/analytics/top-books")
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyticsTopBooks(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> orders = backendGateway.getList(backendGateway.serviceUrls().order() + "/orders");
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        Map<String, Map<String, Object>> booksById = books.stream()
                .collect(Collectors.toMap(
                        book -> asString(firstNonNull(book, "bookId", "id"), ""),
                        book -> book,
                        (left, right) -> left
                ));

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Double> revenue = new LinkedHashMap<>();
        Map<String, String> titles = new LinkedHashMap<>();
        Map<String, String> authors = new LinkedHashMap<>();

        for (Map<String, Object> order : orders) {
            Object bookObject = order.get("book");
            if (!(bookObject instanceof Map<?, ?> rawBookMap)) {
                continue;
            }

            Map<String, Object> orderBook = (Map<String, Object>) rawBookMap;
            String bookId = asString(firstNonNull(orderBook, "productId", "bookId", "id"), "");
            if (bookId.isBlank()) {
                continue;
            }

            int quantity = asInt(firstNonNull(order, "quantity", "qty"), 1);
            double amount = asDouble(firstNonNull(order, "amountPaid", "total", "amount"), 0.0);
            Map<String, Object> bookDetails = booksById.getOrDefault(bookId, Map.of());

            counts.put(bookId, counts.getOrDefault(bookId, 0) + quantity);
            revenue.put(bookId, revenue.getOrDefault(bookId, 0.0) + amount);
            titles.put(bookId, asString(firstNonNull(bookDetails, "title", "productName", "name"), "Book #" + bookId));
            authors.put(bookId, asString(firstNonNull(bookDetails, "author", "authorName"), "Unknown Author"));
        }

        List<Map<String, Object>> topBooks = counts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("bookId", entry.getKey());
                    item.put("title", titles.getOrDefault(entry.getKey(), "Book #" + entry.getKey()));
                    item.put("author", authors.getOrDefault(entry.getKey(), "Unknown Author"));
                    item.put("soldCount", entry.getValue());
                    item.put("revenue", revenue.getOrDefault(entry.getKey(), 0.0));
                    return item;
                })
                .sorted((left, right) -> Double.compare(asDouble(right.get("revenue"), 0.0), asDouble(left.get("revenue"), 0.0)))
                .limit(limit)
                .collect(Collectors.toList());
        return Map.of("topBooks", topBooks);
    }

    @GetMapping("/analytics/low-stock")
    public Map<String, Object> analyticsLowStock(@RequestParam(defaultValue = "10") int threshold) {
        List<Map<String, Object>> books = backendGateway.getList(backendGateway.serviceUrls().book() + "/books");
        List<Map<String, Object>> low = books.stream().filter(b -> {
            Object s = b.get("stock");
            int stock = s instanceof Number ? ((Number) s).intValue() : Integer.parseInt(String.valueOf(s == null ? "0" : s));
            return stock <= threshold;
        }).map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bookId", firstNonNull(b, "bookId", "id"));
            m.put("title", b.get("title"));
            m.put("stock", b.get("stock"));
            return m;
        }).collect(Collectors.toList());
        return Map.of("lowStock", low);
    }

    @GetMapping("/reviews")
    public Map<String, Object> viewAllReviews() {
        return Map.of("reviews", backendGateway.getList(backendGateway.serviceUrls().review() + "/reviews"));
    }

    @PostMapping("/review/moderate")
    public Map<String, Object> moderateReview(@RequestParam Long reviewId) {
        try {
            backendGateway.deleteStrict(backendGateway.serviceUrls().review() + "/reviews/" + reviewId);
            return Map.of("status", "success", "message", "Review removed.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    @GetMapping("/inventory")
    public Map<String, Object> viewInventory() {
        return Map.of("books", backendGateway.getList(backendGateway.serviceUrls().book() + "/books"));
    }

    @PostMapping("/inventory/update")
    public Map<String, Object> updateStock(@RequestParam Long id,
                                           @RequestParam int stock) {
        try {
            backendGateway.putStrict(
                    backendGateway.serviceUrls().book() + "/books/" + id + "/stock?stock=" + stock,
                    null);
            return Map.of("status", "success", "message", "Stock updated successfully.");
        } catch (BackendGatewayException ex) {
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    private boolean isAdmin(String authorizationHeader) {
        StorefrontUser currentUser = currentUserService.currentUser(authorizationHeader);
        return currentUser.authenticated() && currentUser.isAdmin();
    }

    private boolean matchesSearch(Map<String, Object> book, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.trim().toLowerCase();
        return List.of("title", "author", "isbn", "publisher", "genre").stream()
                .map(book::get)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::toLowerCase)
                .anyMatch(value -> value.contains(query));
    }

    private boolean matchesGenre(Map<String, Object> book, String genre) {
        if (genre == null || genre.isBlank()) {
            return true;
        }
        return genre.equalsIgnoreCase(asString(book.get("genre"), ""));
    }

    private boolean matchesStockStatus(Map<String, Object> book, String stockStatus) {
        int stock = asInt(book.get("stock"), 0);
        String normalizedStatus = stockStatus == null ? "all" : stockStatus.trim().toLowerCase();
        return switch (normalizedStatus) {
            case "out" -> stock == 0;
            case "low" -> stock > 0 && stock <= LOW_STOCK_THRESHOLD;
            case "healthy" -> stock > LOW_STOCK_THRESHOLD;
            default -> true;
        };
    }

    private Map<String, Object> sanitizeBookPayload(Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", normalizeText(request.get("title")));
        payload.put("author", normalizeText(request.get("author")));
        payload.put("isbn", normalizeText(request.get("isbn")));
        payload.put("genre", normalizeText(request.get("genre")));
        payload.put("publisher", normalizeText(request.get("publisher")));
        payload.put("price", asDouble(request.get("price"), 0.0));
        payload.put("stock", asInt(request.get("stock"), 0));
        payload.put("rating", asDouble(request.get("rating"), 0.0));
        payload.put("description", normalizeText(request.get("description")));
        payload.put("coverImageUrl", normalizeText(request.get("coverImageUrl")));
        payload.put("publishedDate", normalizeText(request.get("publishedDate")));
        return payload;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Object firstNonNull(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? fallback : text;
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }
}
