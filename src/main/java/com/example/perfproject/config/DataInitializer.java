package com.example.perfproject.config;

import com.example.perfproject.model.Author;
import com.example.perfproject.model.Book;
import com.example.perfproject.repository.AuthorRepository;
import com.example.perfproject.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    @Override
    public void run(String... args) {
        if (authorRepository.count() > 0) {
            return;
        }
        log.info("Khởi tạo dữ liệu mẫu cho ứng dụng...");

        List<Author> authors = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Author author = new Author();
            author.setName("Tác giả " + i);
            authors.add(author);
        }
        authors = authorRepository.saveAll(authors);

        List<Book> books = new ArrayList<>();
        int[] years = {2020, 2021, 2022, 2023, 2024};
        for (int i = 0; i < authors.size(); i++) {
            Author author = authors.get(i);
            for (int j = 1; j <= 5; j++) {
                Book book = new Book();
                book.setTitle("Sách " + ((i * 5) + j));
                book.setPublicationYear(years[j % years.length]);
                book.setAuthor(author);
                books.add(book);
            }
        }
        bookRepository.saveAll(books);
        log.info("Đã tạo {} tác giả và {} cuốn sách thành công!", authors.size(), books.size());
    }
}
