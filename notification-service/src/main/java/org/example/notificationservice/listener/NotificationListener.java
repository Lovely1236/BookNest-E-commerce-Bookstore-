package org.example.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.dto.NotificationMessage;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${notification.queue}")
    public void handleNotificationMessage(@Payload NotificationMessage msg) {
        Notification n = new Notification();
        n.setUserId(msg.getUserId());
        n.setType(msg.getType());
        n.setMessage(msg.getMessage());
        notificationService.sendNotification(n);
    }
}
