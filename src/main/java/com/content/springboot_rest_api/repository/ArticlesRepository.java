package com.content.springboot_rest_api.repository;

import com.content.springboot_rest_api.entity.Article;
import com.content.springboot_rest_api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticlesRepository extends JpaRepository<Article, Long> {
    // Cari artikel berdasarkan Category
    List<Article> findByCategory(Category category);

    Optional<Article> findBySlug(String slug);
}
