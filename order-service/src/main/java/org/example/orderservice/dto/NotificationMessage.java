package org.example.orderservice.dto;

import lombok.Data;

@Data
public class NotificationMessage {
    private Long userId;
    private String type;
    private String message;
}
