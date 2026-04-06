package org.example.bookservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookDto {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String isbn;
    private String genre;
    private String publisher;

    @Positive
    private double price;

    @Min(0)
    private int stock;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private double rating;

    private String description;
    private String coverImageUrl;
    private LocalDate publishedDate;
}