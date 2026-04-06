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
        existing.setGenre(book.getGenre());
        existing.setPrice(book.getPrice());
        existing.setStock(book.getStock());

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

    public List<Book> getFeaturedBooks() {
        return repo.findAll().stream()
                .filter(b -> b.getRating() >= 4.0)
                .toList();
    }
}