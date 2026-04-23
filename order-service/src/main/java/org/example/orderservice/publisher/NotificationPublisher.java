package org.example.orderservice.publisher;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.exchange}")
    private String exchange;

    @Value("${notification.routing-key}")
    private String routingKey;

    public void publish(NotificationMessage msg) {
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }
}
