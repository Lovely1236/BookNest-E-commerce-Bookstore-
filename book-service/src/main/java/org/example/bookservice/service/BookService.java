package org.example.bookservice.service;

import org.example.bookservice.entity.Book;

import java.util.List;

public interface BookService {

    Book addBook(Book book);

    List<Book> getAllBooks();

    Book getBookById(Long id);

    List<Book> searchBooks(String keyword);

    List<Book> getByGenre(String genre);

    Book updateBook(Long id, Book book);

    void deleteBook(Long id);

    Book updateStock(Long id, int stock);

    Book deductStock(Long id, int quantity);

    Book restoreStock(Long id, int quantity);

    List<Book> getFeaturedBooks();
}