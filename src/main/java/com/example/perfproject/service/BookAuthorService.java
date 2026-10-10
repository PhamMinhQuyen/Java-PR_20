package com.example.perfproject.service;

import com.example.perfproject.dto.AuthorResponse;
import com.example.perfproject.dto.BookResponse;
import com.example.perfproject.model.Author;
import com.example.perfproject.model.Book;
import com.example.perfproject.repository.AuthorRepository;
import com.example.perfproject.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookAuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    /**
     * PHƯƠNG THỨC GÂY LỖI N+1 QUERY:
     * - 1 câu query lấy tất cả tác giả: SELECT * FROM author;
     * - N câu query khi truy cập author.getBooks() trong vòng lặp để lấy tên sách:
     *   SELECT * FROM book WHERE author_id = ?; (chạy N lần)
     */
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthorsProblematic() {
        List<Author> authors = authorRepository.findAll();
        List<AuthorResponse> responses = new ArrayList<>();

        for (Author author : authors) {
            // Điểm kích hoạt lazy loading dẫn tới N query phát sinh
            List<String> bookTitles = author.getBooks().stream()
                    .map(Book::getTitle)
                    .collect(Collectors.toList());

            responses.add(AuthorResponse.builder()
                    .id(author.getId())
                    .name(author.getName())
                    .bookCount(bookTitles.size())
                    .bookTitles(bookTitles)
                    .build());
        }
        return responses;
    }

    /**
     * PHƯƠNG THỨC ĐÃ TỐI ƯU VỚI JOIN FETCH:
     * - Chỉ 1 câu truy vấn SQL duy nhất:
     *   SELECT DISTINCT a.*, b.* FROM author a LEFT OUTER JOIN book b ON a.id = b.author_id;
     */
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthorsOptimizedJoinFetch() {
        List<Author> authors = authorRepository.findAllWithBooksJoinFetch();
        return authors.stream()
                .map(author -> AuthorResponse.builder()
                        .id(author.getId())
                        .name(author.getName())
                        .bookCount(author.getBooks().size())
                        .bookTitles(author.getBooks().stream().map(Book::getTitle).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * PHƯƠNG THỨC ĐÃ TỐI ƯU VỚI @EntityGraph:
     * - Chỉ 1 câu truy vấn SQL duy nhất tương tự JOIN FETCH.
     */
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthorsOptimizedEntityGraph() {
        List<Author> authors = authorRepository.findAllWithBooksEntityGraph();
        return authors.stream()
                .map(author -> AuthorResponse.builder()
                        .id(author.getId())
                        .name(author.getName())
                        .bookCount(author.getBooks().size())
                        .bookTitles(author.getBooks().stream().map(Book::getTitle).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm sách theo năm xuất bản
     */
    @Transactional(readOnly = true)
    public List<BookResponse> findBooksByYear(int year) {
        return bookRepository.findByPublicationYear(year).stream()
                .map(b -> BookResponse.builder()
                        .id(b.getId())
                        .title(b.getTitle())
                        .publicationYear(b.getPublicationYear())
                        .authorName(b.getAuthor() != null ? b.getAuthor().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm sách theo năm xuất bản và JOIN FETCH Author (tránh N+1 ngược)
     */
    @Transactional(readOnly = true)
    public List<BookResponse> findBooksByYearWithAuthor(int year) {
        return bookRepository.findByPublicationYearWithAuthor(year).stream()
                .map(b -> BookResponse.builder()
                        .id(b.getId())
                        .title(b.getTitle())
                        .publicationYear(b.getPublicationYear())
                        .authorName(b.getAuthor() != null ? b.getAuthor().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }
}
