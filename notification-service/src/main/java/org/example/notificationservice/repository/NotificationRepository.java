package org.example.notificationservice.repository;

import org.example.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndIsRead(Long userId, boolean isRead);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    List<Notification> findByType(String type);

    void deleteByNotificationId(Long id);
}
