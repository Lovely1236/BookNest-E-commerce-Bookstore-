package org.example.bookservice.service;

import org.example.bookservice.entity.Book;
import org.example.bookservice.exception.ResourceNotFoundException;
import org.example.bookservice.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private Book testBook2;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setBookId(1L);
        testBook.setTitle("Clean Code");
        testBook.setAuthor("Robert C. Martin");
        testBook.setIsbn("978-0132350884");
        testBook.setGenre("Programming");
        testBook.setPublisher("Prentice Hall");
        testBook.setPrice(45.99);
        testBook.setStock(100);
        testBook.setRating(4.8);
        testBook.setDescription("A handbook of agile software craftsmanship");
        testBook.setCoverImageUrl("http://example.com/cover.jpg");
        testBook.setPublishedDate(LocalDate.of(2008, 8, 1));

        testBook2 = new Book();
        testBook2.setBookId(2L);
        testBook2.setTitle("The Pragmatic Programmer");
        testBook2.setAuthor("David Thomas");
        testBook2.setIsbn("978-0201616224");
        testBook2.setGenre("Programming");
        testBook2.setPublisher("Addison Wesley");
        testBook2.setPrice(39.99);
        testBook2.setStock(50);
        testBook2.setRating(4.7);
        testBook2.setDescription("Your journey to mastery");
        testBook2.setCoverImageUrl("http://example.com/cover2.jpg");
        testBook2.setPublishedDate(LocalDate.of(1999, 10, 1));
    }

    // Tests for addBook
    @Test
    void addBook_shouldSaveAndReturnBook() {
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book result = bookService.addBook(testBook);

        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());
        assertEquals("Robert C. Martin", result.getAuthor());
        verify(bookRepository, times(1)).save(testBook);
    }

    // Tests for getAllBooks
    @Test
    void getAllBooks_shouldReturnListOfBooks() {
        List<Book> books = Arrays.asList(testBook, testBook2);
        when(bookRepository.findAll()).thenReturn(books);

        List<Book> result = bookService.getAllBooks();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
        assertEquals("The Pragmatic Programmer", result.get(1).getTitle());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void getAllBooks_shouldReturnEmptyListWhenNoBooks() {
        when(bookRepository.findAll()).thenReturn(Arrays.asList());

        List<Book> result = bookService.getAllBooks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookRepository, times(1)).findAll();
    }

    // Tests for getBookById
    @Test
    void getBookById_shouldReturnBookWhenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        Book result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals("Clean Code", result.getTitle());
        assertEquals(1L, result.getBookId());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    void getBookById_shouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.getBookById(99L)
        );

        assertEquals("Book not found: 99", exception.getMessage());
        verify(bookRepository, times(1)).findById(99L);
    }

    // Tests for searchBooks
    @Test
    void searchBooks_shouldReturnBooksMatchingKeyword() {
        List<Book> foundBooks = Arrays.asList(testBook);
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("Clean", "Clean"))
                .thenReturn(foundBooks);

        List<Book> result = bookService.searchBooks("Clean");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).getTitle());
        verify(bookRepository, times(1))
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("Clean", "Clean");
    }

    @Test
    void searchBooks_shouldReturnEmptyListWhenNoMatch() {
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("Unknown", "Unknown"))
                .thenReturn(Arrays.asList());

        List<Book> result = bookService.searchBooks("Unknown");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Tests for getByGenre
    @Test
    void getByGenre_shouldReturnBooksOfSpecificGenre() {
        List<Book> programmingBooks = Arrays.asList(testBook, testBook2);
        when(bookRepository.findByGenreIgnoreCase("Programming")).thenReturn(programmingBooks);

        List<Book> result = bookService.getByGenre("Programming");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> b.getGenre().equals("Programming")));
        verify(bookRepository, times(1)).findByGenreIgnoreCase("Programming");
    }

    // Tests for updateBook
    @Test
    void updateBook_shouldUpdateBookWhenFound() {
        Book updatedData = new Book();
        updatedData.setTitle("Clean Code - Updated");
        updatedData.setAuthor("Uncle Bob");
        updatedData.setIsbn("978-0132350884");
        updatedData.setGenre("Software Engineering");
        updatedData.setPublisher("Prentice Hall");
        updatedData.setPrice(50.99);
        updatedData.setStock(150);
        updatedData.setRating(4.9);
        updatedData.setDescription("Updated description");
        updatedData.setCoverImageUrl("http://example.com/cover-new.jpg");
        updatedData.setPublishedDate(LocalDate.of(2008, 8, 1));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book result = bookService.updateBook(1L, updatedData);

        assertNotNull(result);
        assertEquals("Clean Code - Updated", result.getTitle());
        assertEquals("Uncle Bob", result.getAuthor());
        assertEquals(50.99, result.getPrice());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void updateBook_shouldThrowExceptionWhenBookNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.updateBook(99L, testBook)
        );

        verify(bookRepository, times(1)).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    // Tests for deleteBook
    @Test
    void deleteBook_shouldDeleteBookById() {
        bookService.deleteBook(1L);

        verify(bookRepository, times(1)).deleteById(1L);
    }

    // Tests for updateStock
    @Test
    void updateStock_shouldUpdateBookStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        bookService.updateStock(1L, 200);

        assertEquals(200, testBook.getStock());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(testBook);
    }

    // Tests for deductStock
    @Test
    void deductStock_shouldReduceStockWhenSufficient() {
        testBook.setStock(100);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book result = bookService.deductStock(1L, 30);

        assertEquals(70, testBook.getStock());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(testBook);
    }

    @Test
    void deductStock_shouldThrowExceptionWhenInsufficientStock() {
        testBook.setStock(10);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookService.deductStock(1L, 50)
        );

        assertEquals("Insufficient stock available for book: 1", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void deductStock_shouldThrowExceptionWhenStockBecomesNegative() {
        testBook.setStock(20);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> bookService.deductStock(1L, 25)
        );

        assertEquals("Insufficient stock available for book: 1", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    // Tests for restoreStock
    @Test
    void restoreStock_shouldIncreaseStock() {
        testBook.setStock(50);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        Book result = bookService.restoreStock(1L, 30);

        assertEquals(80, testBook.getStock());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(testBook);
    }

    @Test
    void restoreStock_shouldThrowExceptionWhenBookNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.restoreStock(99L, 10)
        );

        verify(bookRepository, never()).save(any(Book.class));
    }
}
