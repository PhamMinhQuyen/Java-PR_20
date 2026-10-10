package com.example.perfproject.repository;

import com.example.perfproject.model.Author;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // 1. Phương thức mặc định findAll() kế thừa từ JpaRepository
    // Gây ra lỗi N+1 Query nếu duyệt qua thuộc tính books của từng tác giả

    // 2. GIẢI PHÁP 1: Sử dụng JOIN FETCH trong JPQL
    // Chỉ tạo ra 1 câu truy vấn SQL duy nhất kết hợp bảng author và book
    @Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books")
    List<Author> findAllWithBooksJoinFetch();

    // 3. GIẢI PHÁP 2: Sử dụng @EntityGraph
    // Cho phép cấu hình eager fetch một cách linh hoạt tại repository level mà không thay đổi model
    @EntityGraph(attributePaths = {"books"})
    @Query("SELECT a FROM Author a")
    List<Author> findAllWithBooksEntityGraph();
}
