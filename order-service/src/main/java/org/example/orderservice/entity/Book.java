package org.example.orderservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class Book {

    private Long productId;
    private String productName;
}
