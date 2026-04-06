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

    List<Book> getFeaturedBooks();
}