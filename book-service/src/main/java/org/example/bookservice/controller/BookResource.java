package org.example.bookservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookservice.dto.BookDto;
import org.example.bookservice.entity.Book;
import org.example.bookservice.mapper.BookMapper;
import org.example.bookservice.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookResource {

    private final BookService service;

    @PostMapping
    public ResponseEntity<Book> add(@Valid @RequestBody BookDto dto) {
        return ResponseEntity.ok(service.addBook(BookMapper.toEntity(dto)));
    }

    @GetMapping
    public List<Book> all() {
        return service.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book one(@PathVariable Long id) {
        return service.getBookById(id);
    }

    @GetMapping("/search")
    public List<Book> search(@RequestParam String keyword) {
        return service.searchBooks(keyword);
    }

    @GetMapping("/genre/{genre}")
    public List<Book> genre(@PathVariable String genre) {
        return service.getByGenre(genre);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @RequestBody Book book) {
        return service.updateBook(id, book);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteBook(id);
        return "Deleted";
    }

    @PutMapping("/{id}/stock")
    public Book updateStock(@PathVariable Long id, @RequestParam int stock) {
        return service.updateStock(id, stock);
    }

    @GetMapping("/featured")
    public List<Book> featured() {
        return service.getFeaturedBooks();
    }
}