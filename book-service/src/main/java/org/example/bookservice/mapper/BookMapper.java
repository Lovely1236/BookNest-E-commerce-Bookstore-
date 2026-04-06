package org.example.bookservice.mapper;

import org.example.bookservice.dto.BookDto;
import org.example.bookservice.entity.Book;

public class BookMapper {

    public static Book toEntity(BookDto dto) {
        return Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .isbn(dto.getIsbn())
                .genre(dto.getGenre())
                .publisher(dto.getPublisher())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .rating(dto.getRating())
                .description(dto.getDescription())
                .coverImageUrl(dto.getCoverImageUrl())
                .publishedDate(dto.getPublishedDate())
                .build();
    }
}