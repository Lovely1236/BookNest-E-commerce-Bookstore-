package org.example.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationResource {

    private final NotificationService notificationService;

    @PostMapping
    public Notification send(@RequestBody Notification notification) {
        return notificationService.sendNotification(notification);
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getByUser(@PathVariable Long userId) {
        return notificationService.getByUser(userId);
    }

    @PutMapping("/read/{id}")
    public String markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "Marked as read";
    }

    @PutMapping("/readAll/{userId}")
    public String markAllRead(@PathVariable Long userId) {
        notificationService.markAllRead(userId);
        return "All marked as read";
    }

    @GetMapping("/unread/{userId}")
    public long unreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return "Deleted";
    }

    @GetMapping
    public List<Notification> getAll() {
        return notificationService.getAll();
    }
}
