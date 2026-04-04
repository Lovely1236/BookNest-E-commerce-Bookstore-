package org.example.authservice.entity;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;
    private String email;
    private String passwordHash;
    private String role;
    private String provider;
    private Long mobile;
    private LocalDateTime createdAt;
}