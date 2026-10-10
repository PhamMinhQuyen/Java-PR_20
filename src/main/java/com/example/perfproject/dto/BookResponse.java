package com.example.perfproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private int publicationYear;
    private String authorName;
}
