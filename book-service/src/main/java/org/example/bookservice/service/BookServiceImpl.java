package org.example.bookservice.service;

import lombok.RequiredArgsConstructor;
import org.example.bookservice.entity.Book;
import org.example.bookservice.exception.ResourceNotFoundException;
import org.example.bookservice.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository repo;

    public Book addBook(Book book) {
        return repo.save(book);
    }

    public List<Book> getAllBooks() {
        return repo.findAll();
    }

    public Book getBookById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
    }

    public List<Book> searchBooks(String keyword) {
        return repo.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword);
    }

    public List<Book> getByGenre(String genre) {
        return repo.findByGenreIgnoreCase(genre);
    }

    public Book updateBook(Long id, Book book) {
        Book existing = getBookById(id);

        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());
        existing.setIsbn(book.getIsbn());
        existing.setGenre(book.getGenre());
        existing.setPublisher(book.getPublisher());
        existing.setPrice(book.getPrice());
        existing.setStock(book.getStock());
        existing.setRating(book.getRating());
        existing.setDescription(book.getDescription());
        existing.setCoverImageUrl(book.getCoverImageUrl());
        existing.setPublishedDate(book.getPublishedDate());

        return repo.save(existing);
    }

    public void deleteBook(Long id) {
        repo.deleteById(id);
    }

    public Book updateStock(Long id, int stock) {
        Book book = getBookById(id);
        book.setStock(stock);
        return repo.save(book);
    }

    public Book deductStock(Long id, int quantity) {
        Book book = getBookById(id);
        int newStock = book.getStock() - quantity;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock available for book: " + id);
        }
        book.setStock(newStock);
        return repo.save(book);
    }

    public Book restoreStock(Long id, int quantity) {
        Book book = getBookById(id);
        book.setStock(book.getStock() + quantity);
        return repo.save(book);
    }

    public List<Book> getFeaturedBooks() {
        return repo.findAll().stream()
                .filter(b -> b.getRating() >= 4.0)
                .toList();
    }
}
