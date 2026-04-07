package org.example.notificationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.repository.NotificationRepository;
import org.example.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification sendNotification(Notification notification) {
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Override
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found with id " + id));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    public List<Notification> getByUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Override
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id " + id);
        }
        notificationRepository.deleteByNotificationId(id);
    }

    @Override
    public void sendEmailAlert(String to, String subject, String message) {
        System.out.println("Email Sent to " + to + " | Subject: " + subject + " | Message: " + message);
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }
}
