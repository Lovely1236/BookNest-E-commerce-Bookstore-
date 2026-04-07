package org.example.cartservice.client;

import org.example.cartservice.dto.Book;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/books/{id}")
    Book getBookById(@PathVariable("id") Long id);
}