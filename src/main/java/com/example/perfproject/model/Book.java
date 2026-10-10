package com.example.perfproject.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(
    name = "book",
    indexes = {
        @Index(name = "idx_book_publication_year", columnList = "publicationYear"),
        @Index(name = "idx_book_author_id", columnList = "author_id")
    }
)
@Data
@EqualsAndHashCode(exclude = "author")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private int publicationYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @ToString.Exclude
    private Author author;
}
