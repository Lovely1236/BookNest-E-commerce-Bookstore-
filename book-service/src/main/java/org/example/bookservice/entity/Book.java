package org.example.bookservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    private String title;
    private String author;
    private String isbn;
    private String genre;
    private String publisher;

    private double price;
    private int stock;
    private double rating;

    @Column(length = 2000)
    private String description;

    private String coverImageUrl;

    private LocalDate publishedDate;
}