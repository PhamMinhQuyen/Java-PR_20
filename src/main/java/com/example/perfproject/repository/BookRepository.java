package com.example.perfproject.repository;

import com.example.perfproject.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Tìm kiếm sách theo năm xuất bản.
    // Đã được tối ưu nhờ đánh index: @Index(name = "idx_book_publication_year", columnList = "publicationYear")
    List<Book> findByPublicationYear(int publicationYear);

    // Tìm kiếm sách theo tác giả
    // Đã được tối ưu nhờ đánh index: @Index(name = "idx_book_author_id", columnList = "author_id")
    List<Book> findByAuthorId(Long authorId);

    // Truy vấn kết hợp JOIN FETCH tác giả để tránh N+1 theo chiều ngược lại
    @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.publicationYear = :publicationYear")
    List<Book> findByPublicationYearWithAuthor(@Param("publicationYear") int publicationYear);
}
