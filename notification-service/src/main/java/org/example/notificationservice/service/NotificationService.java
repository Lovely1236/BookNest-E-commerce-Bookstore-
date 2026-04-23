package org.example.notificationservice.service;

import org.example.notificationservice.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification sendNotification(Notification notification);

    void markAsRead(Long id);

    void markAllRead(Long userId);

    List<Notification> getByUser(Long userId);

    long getUnreadCount(Long userId);

    void deleteNotification(Long id);

    void sendEmailAlert(String to, String subject, String message);

    List<Notification> getAll();
}
