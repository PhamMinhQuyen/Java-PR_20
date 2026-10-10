package com.example.perfproject;

import com.example.perfproject.dto.AuthorResponse;
import com.example.perfproject.dto.BookResponse;
import com.example.perfproject.model.Author;
import com.example.perfproject.model.Book;
import com.example.perfproject.repository.AuthorRepository;
import com.example.perfproject.repository.BookRepository;
import com.example.perfproject.service.BookAuthorService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PerformanceBenchmarkTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookAuthorService bookAuthorService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final int NUM_AUTHORS = 200;
    private static final int BOOKS_PER_AUTHOR = 5;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAllInBatch();
        authorRepository.deleteAllInBatch();

        // Seed authors and books
        List<Author> authors = new ArrayList<>();
        for (int i = 1; i <= NUM_AUTHORS; i++) {
            Author author = new Author();
            author.setName("Author " + i);
            authors.add(author);
        }
        authors = authorRepository.saveAll(authors);

        List<Book> books = new ArrayList<>();
        int[] years = {2018, 2019, 2020, 2021, 2022, 2023, 2024};
        for (int i = 0; i < authors.size(); i++) {
            Author author = authors.get(i);
            for (int j = 1; j <= BOOKS_PER_AUTHOR; j++) {
                Book book = new Book();
                book.setTitle("Book " + ((i * BOOKS_PER_AUTHOR) + j));
                book.setPublicationYear(years[j % years.length]);
                book.setAuthor(author);
                books.add(book);
            }
        }
        bookRepository.saveAll(books);

        // Chèn thêm 20,000 bản ghi sách để kiểm tra Index hiệu năng
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 1; i <= 20000; i++) {
            Long authorId = authors.get(i % authors.size()).getId();
            batchArgs.add(new Object[]{"Bulk Title " + i, 1990 + (i % 35), authorId});
        }
        jdbcTemplate.batchUpdate("INSERT INTO book (title, publication_year, author_id) VALUES (?, ?, ?)", batchArgs);
    }

    private Statistics getHibernateStatistics() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Test
    @DisplayName("Benchmark N+1 Problem: So sánh trước và sau khi tối ưu")
    void benchmarkNPlusOneProblem() {
        Statistics stats = getHibernateStatistics();
        stats.setStatisticsEnabled(true);

        // ==========================================
        // 1. TRƯỚC KHI TỐI ƯU (Bị N+1 Query)
        // ==========================================
        stats.clear();
        entityManager.clear();

        long startBefore = System.nanoTime();
        List<AuthorResponse> problematicResult = bookAuthorService.getAuthorsProblematic();
        long durationBeforeMs = (System.nanoTime() - startBefore) / 1_000_000;
        long queryCountBefore = stats.getPrepareStatementCount();

        // ==========================================
        // 2. SAU KHI TỐI ƯU (Dùng JOIN FETCH)
        // ==========================================
        stats.clear();
        entityManager.clear();

        long startAfterFetch = System.nanoTime();
        List<AuthorResponse> joinFetchResult = bookAuthorService.getAuthorsOptimizedJoinFetch();
        long durationAfterFetchMs = (System.nanoTime() - startAfterFetch) / 1_000_000;
        long queryCountAfterFetch = stats.getPrepareStatementCount();

        // ==========================================
        // 3. SAU KHI TỐI ƯU (Dùng @EntityGraph)
        // ==========================================
        stats.clear();
        entityManager.clear();

        long startAfterGraph = System.nanoTime();
        List<AuthorResponse> entityGraphResult = bookAuthorService.getAuthorsOptimizedEntityGraph();
        long durationAfterGraphMs = (System.nanoTime() - startAfterGraph) / 1_000_000;
        long queryCountAfterGraph = stats.getPrepareStatementCount();

        System.out.println("==========================================================================");
        System.out.println("                 KẾT QUẢ BENCHMARK N+1 QUERY (200 Authors)                ");
        System.out.println("==========================================================================");
        System.out.printf("%-30s | %-15s | %-15s%n", "Phương pháp", "Số câu Query", "Thời gian (ms)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-30s | %-15d | %-15d%n", "Trước tối ưu (Mặc định)", queryCountBefore, durationBeforeMs);
        System.out.printf("%-30s | %-15d | %-15d%n", "Sau tối ưu (JOIN FETCH)", queryCountAfterFetch, durationAfterFetchMs);
        System.out.printf("%-30s | %-15d | %-15d%n", "Sau tối ưu (@EntityGraph)", queryCountAfterGraph, durationAfterGraphMs);
        System.out.println("==========================================================================");

        // Kiểm tra
        assertTrue(queryCountBefore > 1, "Trước khi tối ưu phải có nhiều câu query (N+1)");
        assertTrue(queryCountAfterFetch == 1, "Sau khi JOIN FETCH chỉ còn 1 query duy nhất");
        assertTrue(queryCountAfterGraph == 1, "Sau khi @EntityGraph chỉ còn 1 query duy nhất");
        assertFalse(problematicResult.isEmpty());
        assertFalse(joinFetchResult.isEmpty());
    }

    private void dropIndexSafely() {
        try {
            jdbcTemplate.execute("ALTER TABLE book DROP INDEX idx_book_publication_year");
        } catch (Exception e1) {
            try {
                jdbcTemplate.execute("DROP INDEX IF EXISTS idx_book_publication_year");
            } catch (Exception ignored) {}
        }
    }

    private void createIndexSafely() {
        try {
            jdbcTemplate.execute("CREATE INDEX idx_book_publication_year ON book(publication_year)");
        } catch (Exception ignored) {}
    }

    private String getExplainPlan(int targetYear) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "EXPLAIN SELECT id, title, publication_year, author_id FROM book WHERE publication_year = ?", targetYear);
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                if (row.containsKey("type")) {
                    return String.format("type=%s, key=%s, rows=%s",
                            row.get("type"), row.get("key"), row.get("rows"));
                }
            }
        } catch (Exception ignored) {}
        try {
            List<String> list = jdbcTemplate.query(
                    "EXPLAIN SELECT id, title, publication_year, author_id FROM book WHERE publication_year = " + targetYear,
                    (rs, rowNum) -> rs.getString(1)
            );
            return list.isEmpty() ? "N/A" : list.get(0).replaceAll("\n", " ");
        } catch (Exception e) {
            return "N/A";
        }
    }

    @Test
    @DisplayName("Benchmark Index: So sánh trước và sau khi đánh Index cho publication_year")
    void benchmarkIndexPerformance() {
        int targetYear = 2023;

        // Xóa index để đo thời gian TRƯỚC KHI ĐÁNH INDEX (Full Table Scan)
        dropIndexSafely();

        // Warm up
        jdbcTemplate.queryForList("SELECT id FROM book WHERE publication_year = ?", targetYear);

        long startNoIndex = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            jdbcTemplate.queryForList("SELECT id, title, publication_year, author_id FROM book WHERE publication_year = ?", targetYear);
        }
        long durationNoIndexMs = (System.nanoTime() - startNoIndex) / 1_000_000;
        String planNoIndex = getExplainPlan(targetYear);

        // Tạo Index để đo thời gian SAU KHI ĐÁNH INDEX (Index Scan)
        createIndexSafely();

        // Warm up
        jdbcTemplate.queryForList("SELECT id FROM book WHERE publication_year = ?", targetYear);

        long startIndex = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            jdbcTemplate.queryForList("SELECT id, title, publication_year, author_id FROM book WHERE publication_year = ?", targetYear);
        }
        long durationIndexMs = (System.nanoTime() - startIndex) / 1_000_000;
        String planIndex = getExplainPlan(targetYear);

        System.out.println("==========================================================================");
        System.out.println("         KẾT QUẢ BENCHMARK INDEX PUBLICATION_YEAR (21,000 Books)          ");
        System.out.println("==========================================================================");
        System.out.printf("%-30s | %-15s | %-35s%n", "Trạng thái", "20 lần Query (ms)", "Execution Plan");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-30s | %-15d | %-35s%n", "Trước khi có Index (Scan)", durationNoIndexMs, planNoIndex);
        System.out.printf("%-30s | %-15d | %-35s%n", "Sau khi có Index (Range)", durationIndexMs, planIndex);
        System.out.println("==========================================================================");
    }
}
