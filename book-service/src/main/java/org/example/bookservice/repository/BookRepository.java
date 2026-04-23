package org.example.bookservice.repository;

import org.example.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByGenreIgnoreCase(String genre);

    Book findByIsbn(String isbn);

    List<Book> findByPriceBetween(double min, double max);

    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String t, String a);
}