package com.example.perfproject.controller;

import com.example.perfproject.dto.AuthorResponse;
import com.example.perfproject.dto.BookResponse;
import com.example.perfproject.service.BookAuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerformanceController {

    private final BookAuthorService bookAuthorService;

    /**
     * Endpoint mô phỏng truy vấn N+1 Query (Trước khi sửa)
     */
    @GetMapping("/authors/problematic")
    public ResponseEntity<List<AuthorResponse>> getAuthorsProblematic() {
        return ResponseEntity.ok(bookAuthorService.getAuthorsProblematic());
    }

    /**
     * Endpoint tối ưu N+1 Query bằng JOIN FETCH (Sau khi sửa)
     */
    @GetMapping("/authors/join-fetch")
    public ResponseEntity<List<AuthorResponse>> getAuthorsJoinFetch() {
        return ResponseEntity.ok(bookAuthorService.getAuthorsOptimizedJoinFetch());
    }

    /**
     * Endpoint tối ưu N+1 Query bằng @EntityGraph (Sau khi sửa)
     */
    @GetMapping("/authors/entity-graph")
    public ResponseEntity<List<AuthorResponse>> getAuthorsEntityGraph() {
        return ResponseEntity.ok(bookAuthorService.getAuthorsOptimizedEntityGraph());
    }

    /**
     * Endpoint truy vấn sách theo năm (Tận dụng Index)
     */
    @GetMapping("/books/by-year")
    public ResponseEntity<List<BookResponse>> getBooksByYear(@RequestParam(defaultValue = "2023") int year) {
        return ResponseEntity.ok(bookAuthorService.findBooksByYear(year));
    }

    /**
     * Endpoint truy vấn sách theo năm kết hợp JOIN FETCH Author
     */
    @GetMapping("/books/by-year-with-author")
    public ResponseEntity<List<BookResponse>> getBooksByYearWithAuthor(@RequestParam(defaultValue = "2023") int year) {
        return ResponseEntity.ok(bookAuthorService.findBooksByYearWithAuthor(year));
    }
}
