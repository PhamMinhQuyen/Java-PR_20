package com.example.perfproject.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "author")
@Data
@EqualsAndHashCode(exclude = "books")
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Tránh vòng lặp vô hạn khi gọi toString()
    // @BatchSize(size = 50) // Có thể bật nếu muốn giải pháp batch fetching cho lazy collection
    private List<Book> books = new ArrayList<>();
}
