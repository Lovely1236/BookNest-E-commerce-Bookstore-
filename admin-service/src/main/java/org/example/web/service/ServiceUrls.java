package org.example.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceUrls {
    @Value("${services.book:http://book-service:8082}")
    private String book;

    @Value("${services.order:http://order-service:8084}")
    private String order;

    @Value("${services.review:http://review-service:8088}")
    private String review;

    @Value("${services.notification:http://notification-service:8087}")
    private String notification;

    @Value("${services.wishlist:http://wishlist-service:8090}")
    private String wishlist;

    @Value("${services.auth:http://auth-service:8081}")
    private String auth;

    public String book() { return book; }
    public String order() { return order; }
    public String review() { return review; }
    public String notification() { return notification; }
    public String wishlist() { return wishlist; }
    public String auth() { return auth; }
}
